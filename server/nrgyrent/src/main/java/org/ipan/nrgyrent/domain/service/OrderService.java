package org.ipan.nrgyrent.domain.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.NotImplementedException;
import org.ipan.nrgyrent.domain.exception.AutodelegateReserveExceededException;
import org.ipan.nrgyrent.domain.exception.OrderAlreadyExistsException;
import org.ipan.nrgyrent.domain.model.*;
import org.ipan.nrgyrent.domain.model.autodelegation.AutoDelegationSession;
import org.ipan.nrgyrent.domain.model.repository.AutoDelegationSessionRepo;
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
    private final AutoDelegationSessionService autoDelegationSessionService;
    private final AutoDelegationSessionRepo autoDelegationSessionRepo;

    @Transactional
    public Order createAutodelegateOrder(AddOrUpdateOrderCommand command) {
        Order order = createPendingOrder(command);
        order = completeOrder(command);
        return order;
    }

    @Transactional
    public Order createPendingOrder(AddOrUpdateOrderCommand command) {
        EntityManager em = getEntityManager();

        AppUser user = null;
        Tariff tariff = null;
        Balance targetBalance = null;
        Long totalSunAmount = null;

        // Make check only for normal orders
        if (OrderType.USER.equals(command.getType())) {
            user = em.find(AppUser.class, command.getUserId());

            if (user == null) {
                logger.error("User is not found when creating pending order, command {}", command);
                throw new IllegalArgumentException("User not found");
            }

            if (command.getDuration() == null || command.getEnergyAmountPerTx() == null || command.getSunAmountPerTx() == null || command.getTxAmount() == null
                || command.getCorrelationId() == null || command.getReceiveAddress() == null ||command.getItrxFeeSunAmount() == null) {
                    logger.error("Some of the command properties are not set, command {}", command);
                    throw new IllegalArgumentException("Some of the command properties are not set");
            }

            // usually the auto delegation order.
            if (command.getSerial() != null) {
                Order orderWithSameSerial = orderRepo.findBySerialEquals(command.getSerial());
                if (orderWithSameSerial != null) {
                    logger.error("Order with the same serial already exists serial {}", command.getSerial());
                    throw new OrderAlreadyExistsException("Order with the same serial already exists serial");
                }
            }

            // It should not be null but just in case.
            if (command.getTariffId() != null) {
                tariff = tariffRepo.getReferenceById(command.getTariffId());
            }

            totalSunAmount = command.getTxAmount() * command.getSunAmountPerTx();

            targetBalance = user.getBalanceToUse();
            balanceService.subtractSunBalance(targetBalance, totalSunAmount);

            if (command.getAutoDelegationSessionId() == null) {
                AutoDelegationSession session = autoDelegationSessionRepo.findByUserTelegramIdAndActive(user.getTelegramId(), true);
                if (session != null) {
                    // user has active autodelegate session
                    // We need to keep some as a reserve which is transactionType2 cost
                    if (targetBalance.getSunBalance() < tariff.getMaxAutodelegateFee()) {
                        logger.error("The user {} has active session id, and balance after regular transaction is lower than min reserve for auto delegation", user.getTelegramId());
                        throw new AutodelegateReserveExceededException("A user has active session id, and balance after regular transaction is lower than min reserve for auto delegation", tariff.getMaxAutodelegateFee());
                    }
                }
            }

            logger.info("Creating a pending order for user id {} username: {} balance: {} params: {}", user.getTelegramId(), user.getTelegramUsername(), targetBalance.getId(), command);
        } else {
            logger.info("Creating SYSTEM ORDER params: {}", command);
        }

        Integer totalEnergyAmount = command.getTxAmount() * command.getEnergyAmountPerTx();

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
        order.setType(command.getType());

        Long autoTopupSessionId = command.getAutoDelegationSessionId();
        if (autoTopupSessionId != null) {
            order.setAutoDelegationSession(autoDelegationSessionRepo.findById(autoTopupSessionId).get());
        }

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

        if (OrderType.USER.equals(order.getType())) {
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
        }

        return order;
    }

    private Long calculateCommissionAsPercentFromProfit(Order order, ReferralProgram referralProgram) {
        long actualProfitLong = order.getSunAmount() - order.getItrxFeeSunAmount();
        long profitVisible = order.getSunAmount() - referralProgram.getSubtractAmount();

        BigDecimal profit = new BigDecimal(profitVisible);
        BigDecimal commissionVisible = profit
                .divide(AppConstants.HUNDRED)
                .multiply(new BigDecimal(referralProgram.getPercentage()))
                .setScale(0, RoundingMode.DOWN);

        BigDecimal profitAct = new BigDecimal(actualProfitLong);
        BigDecimal commissionActual = profitAct
                .divide(AppConstants.HUNDRED)
                .multiply(new BigDecimal(referralProgram.getPercentage()))
                .setScale(0, RoundingMode.DOWN);

        long baseEnergyAmount = order.getEnergyAmount() / order.getTxAmount();

        // do this only for 65K energy amount orders
        if (baseEnergyAmount == AppConstants.ENERGY_65K) {
            order.setRefProgramProfitRemainder(commissionActual.longValue() - commissionVisible.longValue());
            return commissionVisible.longValue();
        } else {
            return commissionActual.longValue();
        }
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

        if (OrderType.USER.equals(order.getType())) {
            Balance balance = order.getBalance();
            balance.setSunBalance(balance.getSunBalance() + order.getSunAmount());
        }

        return order;
    }


    @Lookup
    public EntityManager getEntityManager() {
        throw new NotImplementedException();
    }
}
