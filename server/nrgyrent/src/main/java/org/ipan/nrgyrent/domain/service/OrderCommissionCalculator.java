package org.ipan.nrgyrent.domain.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@AllArgsConstructor
@Slf4j
public class OrderCommissionCalculator {
    public Long calculateCommissionAsPercentFromRevenue(Order order, ReferralProgram referralProgram) {
        BigDecimal revenue = new BigDecimal(order.getSunAmount());
        BigDecimal commission = revenue
                .divide(AppConstants.HUNDRED)
                .multiply(new BigDecimal(referralProgram.getPercentage()))
                .setScale(0, RoundingMode.DOWN);

        return commission.longValue();
    }

    public Long calculateCommissionAsPercentFromProfit(Order order, ReferralProgram referralProgram) {
        long actualProfitLong = order.getSunAmount() - order.getItrxFeeSunAmount();

        BigDecimal profitAct = new BigDecimal(actualProfitLong);
        BigDecimal commissionActual = profitAct
                .divide(AppConstants.HUNDRED)
                .multiply(new BigDecimal(referralProgram.getPercentage()))
                .setScale(0, RoundingMode.DOWN);

        long baseEnergyAmount = order.getEnergyAmount() / order.getTxAmount();
        boolean useProviderAmount = referralProgram.getSubtractAmountUseProviderAmount();

        // do this only for 65K energy amount orders
        if (!useProviderAmount && baseEnergyAmount == AppConstants.ENERGY_65K) {
            Long transactionCost = getTransaction1Cost(order, referralProgram);

            long profitVisible = order.getSunAmount() - transactionCost * order.getTxAmount();

            BigDecimal profit = new BigDecimal(profitVisible);
            BigDecimal commissionVisible = profit
                    .divide(AppConstants.HUNDRED)
                    .multiply(new BigDecimal(referralProgram.getPercentage()))
                    .setScale(0, RoundingMode.DOWN);

            order.setRefProgramProfitRemainder(actualProfitLong - profitVisible);
            return commissionVisible.longValue();
        } else if (!useProviderAmount && baseEnergyAmount == AppConstants.ENERGY_131K) {
            Long transactionCost = getTransaction2Cost(order, referralProgram);
            long profitVisible = order.getSunAmount() - transactionCost * order.getTxAmount();

            BigDecimal profit = new BigDecimal(profitVisible);
            BigDecimal commissionVisible = profit
                    .divide(AppConstants.HUNDRED)
                    .multiply(new BigDecimal(referralProgram.getPercentage()))
                    .setScale(0, RoundingMode.DOWN);

            order.setRefProgramProfitRemainder(actualProfitLong - profitVisible);
            return commissionVisible.longValue();
        } else {
            return commissionActual.longValue();
        }
    }

    private Long getTransaction1Cost(Order order, ReferralProgram referralProgram) {
        Long result = null;
        if (order.isAutodelegationOrder()) {
            switch (order.getEnergyProvider()) {
                case TRXX:
                case ITRX:
                    result = referralProgram.getSubtractAmountTx1AutoItrx();
                    break;
                default:
                    logger.error("Unknown autodelegation provider type");
            }
        } else {
            switch (order.getEnergyProvider()) {
                case ITRX:
                    result = referralProgram.getSubtractAmountTx1Itrx();
                    break;
                case CATFEE:
                case NETTS:
                    result = referralProgram.getSubtractAmountTx1Catfee();
                    break;
                default:
                    logger.error("Unknown provider type");
            }
        }
        return result;
    }

    private Long getTransaction2Cost(Order order, ReferralProgram referralProgram) {
        Long result = null;
        if (order.isAutodelegationOrder()) {
            switch (order.getEnergyProvider()) {
                case TRXX:
                case ITRX:
                    result = referralProgram.getSubtractAmountTx2AutoItrx();
                    break;
                default:
                    logger.error("Unknown autodelegation provider type");
            }
        } else {
            switch (order.getEnergyProvider()) {
                case ITRX:
                    result = referralProgram.getSubtractAmountTx2Itrx();
                    break;
                case CATFEE:
                case NETTS:
                    result = referralProgram.getSubtractAmountTx2Catfee();
                    break;
                default:
                    logger.error("Unknown provider type");
            }
        }

        return result;
    }
}
