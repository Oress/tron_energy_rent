package org.ipan.nrgyrent.domain.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.model.AppUser;
import org.ipan.nrgyrent.domain.model.Balance;
import org.ipan.nrgyrent.domain.model.BalanceReferralProgram;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.OrderStatus;
import org.ipan.nrgyrent.domain.model.ReferralCommission;
import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.domain.model.ReferralProgramCalcType;
import org.ipan.nrgyrent.domain.model.Tariff;
import org.ipan.nrgyrent.domain.model.repository.OrderRepo;
import org.ipan.nrgyrent.domain.model.repository.ReferralCommissionRepo;
import org.ipan.nrgyrent.domain.model.repository.TariffRepo;
import org.ipan.nrgyrent.domain.service.commands.orders.AddOrUpdateOrderCommand;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@AllArgsConstructor
@Slf4j
public class OrderService {
    private final TariffRepo tariffRepo;
    private final OrderRepo orderRepo;
    private final BalanceService balanceService;
    private final ReferralCommissionRepo referralCommissionRepo;

    @Transactional
    public Order createPendingOrder(AddOrUpdateOrderCommand command) {
        EntityManager em = getEntityManager();

        AppUser user = em.find(AppUser.class, command.getUserId());

        if (user == null) {
            logger.error("User is not found when creating pending order, command {}", command);
            throw new IllegalArgumentException("User not found");
        }

        if (command.getDuration() == null || command.getEnergyAmountPerTx() == null || command.getSunAmountPerTx() == null || command.getTxAmount() == null
            || command.getCorrelationId() == null || command.getReceiveAddress() == null ||command.getItrxFeeSunAmount() == null) {
                logger.error("Some of the command properties are not set, command {}", command);
                throw new IllegalArgumentException("Some of the command properties are not set");
        }

        Tariff tariff = null;
        // It should not be null but just in case.
        if (command.getTariffId() != null) {
            tariff = tariffRepo.getReferenceById(command.getTariffId());
        }

        Long totalSunAmount = command.getTxAmount() * command.getSunAmountPerTx();
        Integer totalEnergyAmount = command.getTxAmount() * command.getEnergyAmountPerTx();

        Balance targetBalance = user.getBalanceToUse();
        balanceService.subtractSunBalance(targetBalance, totalSunAmount);

        logger.info("Creating a pending order for user id {} username: {} balance: {} params: {}", user.getTelegramId(), user.getTelegramUsername(), targetBalance.getId(), command);

        Order order = new Order();
        order.setUser(user);
        order.setBalance(targetBalance);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setDuration(command.getDuration());
        order.setCorrelationId(command.getCorrelationId());
        order.setTxAmount(command.getTxAmount());
        order.setSunAmount(totalSunAmount);
        order.setEnergyAmount(totalEnergyAmount);
        order.setItrxFeeSunAmount(command.getItrxFeeSunAmount());
        order.setReceiveAddress(command.getReceiveAddress());
        order.setMessageToUpdate(command.getMessageIdToUpdate());
        order.setChatId(command.getChatId());
        order.setTariff(tariff);

        em.persist(order);

        return order;
    }

    @Transactional
    public Order completeOrder(AddOrUpdateOrderCommand command) {
        logger.info("Completing domain order: {}", command);
        Optional<Order> byCorrelationId = orderRepo.findByCorrelationId(command.getCorrelationId());

        if (byCorrelationId.isEmpty()) {
            logger.error("Order not found: {}", command.getCorrelationId());
            throw new IllegalArgumentException("Order not found");
        }

        Order order = byCorrelationId.get();
        order.setOrderStatus(OrderStatus.COMPLETED);
        order.setItrxStatus(command.getItrxStatus());
        order.setTxId(command.getTxId());
        order.setSerial(command.getSerial());

        // Generate the referral commission record if user was invited
        AppUser user = order.getUser();
        BalanceReferralProgram balanceReferralProgram = user.getReferralProgram();
        if (balanceReferralProgram != null) {
            logger.info("Generating referral commission record from order {}", order.getId());
            ReferralProgram referralProgram = balanceReferralProgram.getReferralProgram();

            ReferralProgramCalcType calcType = referralProgram.getCalcType();
            Long commissionLong = switch (calcType) {
                case ReferralProgramCalcType.PERCENT_FROM_PROFIT -> calculateCommissionAsPercentFromProfit(order, referralProgram);
                case ReferralProgramCalcType.PERCENT_FROM_REVENUE -> calculateCommissionAsPercentFromRevenue(order, referralProgram);
                default -> 0L;
            };

            if (commissionLong > 0) {
                ReferralCommission referralCommission = new ReferralCommission();
                referralCommission.setAmountSun(commissionLong);
                referralCommission.setCalcType(calcType);
                referralCommission.setOrder(order);
                referralCommission.setBalanceReferralProgram(balanceReferralProgram);
                referralCommission.setPercentage(referralProgram.getPercentage());
                referralCommission.setReferralProgram(referralProgram);
                referralCommissionRepo.save(referralCommission);
            } else {
                logger.error("The commission is negative {} for order id: {}, correlation id{}", commissionLong, order.getId(), command.getCorrelationId());
            }
        }

        return order;
    }

    private Long calculateCommissionAsPercentFromProfit(Order order, ReferralProgram referralProgram) {
        Long profitLong = order.getSunAmount() - order.getItrxFeeSunAmount();
        BigDecimal profit = new BigDecimal(profitLong);
        BigDecimal commission = profit
            .divide(AppConstants.HUNDRED)
            .multiply(new BigDecimal(referralProgram.getPercentage()))
            .setScale(0, RoundingMode.DOWN);

        return commission.longValue();
    }

    private Long calculateCommissionAsPercentFromRevenue(Order order, ReferralProgram referralProgram) {
        BigDecimal revenue = new BigDecimal(order.getSunAmount());
        BigDecimal commission = revenue
            .divide(AppConstants.HUNDRED)
            .multiply(new BigDecimal(referralProgram.getPercentage()))
            .setScale(0, RoundingMode.DOWN);

        return commission.longValue();
    }

    @Transactional
    public Order refundOrder(AddOrUpdateOrderCommand command) {
        logger.info("Refunding order: {}", command);
        Optional<Order> byCorrelationId = orderRepo.findByCorrelationId(command.getCorrelationId());

        if (byCorrelationId.isEmpty()) {
            logger.error("Order not found: {}", command.getCorrelationId());
            throw new IllegalArgumentException("Order not found");
        }

        Order order = byCorrelationId.get();
        order.setOrderStatus(OrderStatus.REFUNDED);
        order.setItrxStatus(command.getItrxStatus());
        order.setSerial(command.getSerial());

        Balance balance = order.getBalance();
        balance.setSunBalance(balance.getSunBalance() + order.getSunAmount());

        return order;
    }


    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
