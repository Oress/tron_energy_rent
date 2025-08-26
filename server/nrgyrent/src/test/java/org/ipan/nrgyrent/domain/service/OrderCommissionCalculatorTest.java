package org.ipan.nrgyrent.domain.service;

import org.assertj.core.api.Assertions;
import org.ipan.nrgyrent.domain.model.EnergyProviderName;
import org.ipan.nrgyrent.domain.model.Order;
import org.ipan.nrgyrent.domain.model.ReferralProgram;
import org.ipan.nrgyrent.itrx.AppConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;

// Cases (simple transactions, ITRX provider, CATFEE provider):
// 8.6 and 5.5 TRX. 5% from income
// 8.6 and 5.5 TRX. 50% from profit with subtract amount
// 8.6 and 5.5 TRX. 50% from profit w/o subtract amount

// Cases (simple auto-delegation, ITRX provider):
// 8.6 and 5.5 TRX. 50% from profit with subtract amount
// 8.6 and 5.5 TRX. 50% from profit with use provider amount flag.

// Assuming: standard tarif is 8.6 TRX for 65K energy and 5.5 TRX for 131K energy,
// provider fee is 1.885 TRX for 65K energy and 3.799 TRX for 131K energy
// autodelegation fee is 2.6 TRX for 65K energy and 5.2 TRX for 131K energy
//@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class OrderCommissionCalculatorTest {
    OrderCommissionCalculator calculator = new OrderCommissionCalculator();

    // 8.6 and 5.5 TRX. 5% from income
    @Test
    public void standardTarif_5pcntFromRevenue() {
        Order orderTx1 = mock(Order.class);
        when(orderTx1.getSunAmount()).thenReturn(AppConstants.PRICE_65K);

        Order orderTx2 = mock(Order.class);
        when(orderTx2.getSunAmount()).thenReturn(AppConstants.PRICE_131K);

        ReferralProgram referralProgram = mockDefault5pcntRp();

        Long commissionTx1 = calculator.calculateCommissionAsPercentFromRevenue(orderTx1, referralProgram);
        Long commissionTx2 = calculator.calculateCommissionAsPercentFromRevenue(orderTx2, referralProgram);

        // 5% from 8.6 TRX = 0.43 TRX
        // 5% from 5.5 TRX = 0.275 TRX
        Assertions.assertThat(commissionTx1).isEqualTo(275_000L);
        Assertions.assertThat(commissionTx2).isEqualTo(430_000L);
    }

    // 8.6 and 5.5 TRX. 50% from profit w/o subtract amount
    @Test
    public void standardTarif_50pcntFromIncomeWOSubtractAmnt() {
        Order orderTx1 = mock(Order.class);
        when(orderTx1.getSunAmount()).thenReturn(AppConstants.PRICE_65K);
        when(orderTx1.getItrxFeeSunAmount()).thenReturn(1_885_000L);
        when(orderTx1.getEnergyAmount()).thenReturn(AppConstants.ENERGY_65K);
        when(orderTx1.getTxAmount()).thenReturn(1);

        Order orderTx2 = mock(Order.class);
        when(orderTx2.getSunAmount()).thenReturn(AppConstants.PRICE_131K);
        when(orderTx2.getItrxFeeSunAmount()).thenReturn(3_799_000L);
        when(orderTx2.getEnergyAmount()).thenReturn(AppConstants.ENERGY_131K);
        when(orderTx2.getTxAmount()).thenReturn(1);

        ReferralProgram referralProgram = mock50pcntWOSubtractAmountRp();

        Long commissionTx1 = calculator.calculateCommissionAsPercentFromProfit(orderTx1, referralProgram);
        Long commissionTx2 = calculator.calculateCommissionAsPercentFromProfit(orderTx2, referralProgram);

        Assertions.assertThat(commissionTx1).isEqualTo(1_807_500L);
        Assertions.assertThat(commissionTx2).isEqualTo(2_400_500L);
    }

    // 8.6 and 5.5 TRX. 50% from profit with subtract amount
    @ParameterizedTest
    @EnumSource(EnergyProviderName.class)
    public void standardTarif_50pcntFromIncomeWithSubtractAmnt(EnergyProviderName energyProvider) {
        Order orderTx1 = mock(Order.class);
        when(orderTx1.getSunAmount()).thenReturn(AppConstants.PRICE_65K);
        when(orderTx1.getItrxFeeSunAmount()).thenReturn(1_885_000L);
        when(orderTx1.getEnergyAmount()).thenReturn(AppConstants.ENERGY_65K);
        when(orderTx1.getTxAmount()).thenReturn(1);
        when(orderTx1.isAutodelegationOrder()).thenReturn(false);
        when(orderTx1.getEnergyProvider()).thenReturn(energyProvider);

        Order orderTx2 = mock(Order.class);
        when(orderTx2.getSunAmount()).thenReturn(AppConstants.PRICE_131K);
        when(orderTx2.getItrxFeeSunAmount()).thenReturn(3_799_000L);
        when(orderTx2.getEnergyAmount()).thenReturn(AppConstants.ENERGY_131K);
        when(orderTx2.getTxAmount()).thenReturn(1);
        when(orderTx2.isAutodelegationOrder()).thenReturn(false);
        when(orderTx2.getEnergyProvider()).thenReturn(energyProvider);

        ArgumentCaptor<Long> captor1 = ArgumentCaptor.captor();
        ArgumentCaptor<Long> captor2 = ArgumentCaptor.captor();
        ReferralProgram referralProgram = mock50pcntWithSubtractAmountRp();

        Long commissionTx1 = calculator.calculateCommissionAsPercentFromProfit(orderTx1, referralProgram);
        verify(orderTx1).setRefProgramProfitRemainder(captor1.capture());

        Long commissionTx2 = calculator.calculateCommissionAsPercentFromProfit(orderTx2, referralProgram);
        verify(orderTx2).setRefProgramProfitRemainder(captor2.capture());

        Assertions.assertThat(commissionTx1).isEqualTo(1_162_500L);
        Assertions.assertThat(captor1.getValue()).isEqualTo(1_290_000L);

        Assertions.assertThat(commissionTx2).isEqualTo(1_125_000L);
        Assertions.assertThat(captor2.getValue()).isEqualTo(2_551_000L);
    }


    // 8.6 and 5.5 TRX. 5% from income
    @Test
    public void auto_standardTarif_5pcntFromRevenue() {
        Order orderTx1 = mock(Order.class);
        when(orderTx1.getSunAmount()).thenReturn(AppConstants.PRICE_65K);
        when(orderTx1.isAutodelegationOrder()).thenReturn(true);

        Order orderTx2 = mock(Order.class);
        when(orderTx2.getSunAmount()).thenReturn(AppConstants.PRICE_131K);
        when(orderTx2.isAutodelegationOrder()).thenReturn(true);

        ReferralProgram referralProgram = mockDefault5pcntRp();

        Long commissionTx1 = calculator.calculateCommissionAsPercentFromRevenue(orderTx1, referralProgram);
        Long commissionTx2 = calculator.calculateCommissionAsPercentFromRevenue(orderTx2, referralProgram);

        // 5% from 8.6 TRX = 0.43 TRX
        // 5% from 5.5 TRX = 0.275 TRX
        Assertions.assertThat(commissionTx1).isEqualTo(275_000L);
        Assertions.assertThat(commissionTx2).isEqualTo(430_000L);
    }

    // 8.6 and 5.5 TRX. 50% from profit w/o subtract amount
    @Test
    public void auto_standardTarif_50pcntFromIncomeWOSubtractAmnt() {
        Order orderTx1 = mock(Order.class);
        when(orderTx1.getSunAmount()).thenReturn(AppConstants.PRICE_65K);
        when(orderTx1.getItrxFeeSunAmount()).thenReturn(2_600_000L);
        when(orderTx1.getEnergyAmount()).thenReturn(AppConstants.ENERGY_131K); // this is how itrx sends event.
        when(orderTx1.getTxAmount()).thenReturn(1);
        when(orderTx1.isAutodelegationOrder()).thenReturn(true);

        Order orderTx2 = mock(Order.class);
        when(orderTx2.getSunAmount()).thenReturn(AppConstants.PRICE_131K);
        when(orderTx2.getItrxFeeSunAmount()).thenReturn(5_200_000L);
        when(orderTx2.getEnergyAmount()).thenReturn(AppConstants.ENERGY_131K);
        when(orderTx2.getTxAmount()).thenReturn(1);
        when(orderTx2.isAutodelegationOrder()).thenReturn(true);

        ReferralProgram referralProgram = mock50pcntWOSubtractAmountRp();

        Long commissionTx1 = calculator.calculateCommissionAsPercentFromProfit(orderTx1, referralProgram);
        Long commissionTx2 = calculator.calculateCommissionAsPercentFromProfit(orderTx2, referralProgram);

        Assertions.assertThat(commissionTx1).isEqualTo(1_450_000L);
        Assertions.assertThat(commissionTx2).isEqualTo(1_700_000L);
    }

    // 8.6 and 5.5 TRX. 50% from profit with subtract amount
    @Test
    public void auto_standardTarif_50pcntFromIncomeWithSubtractAmnt() {
        Order orderTx1 = mock(Order.class);
        when(orderTx1.getSunAmount()).thenReturn(AppConstants.PRICE_65K);
        when(orderTx1.getItrxFeeSunAmount()).thenReturn(2_600_000L);
        when(orderTx1.getEnergyAmount()).thenReturn(AppConstants.ENERGY_65K);
        when(orderTx1.getTxAmount()).thenReturn(1);
        when(orderTx1.getEnergyProvider()).thenReturn(EnergyProviderName.ITRX);
        when(orderTx1.isAutodelegationOrder()).thenReturn(true);


        Order orderTx2 = mock(Order.class);
        when(orderTx2.getSunAmount()).thenReturn(AppConstants.PRICE_131K);
        when(orderTx2.getItrxFeeSunAmount()).thenReturn(5_200_000L);
        when(orderTx2.getEnergyAmount()).thenReturn(AppConstants.ENERGY_131K);
        when(orderTx2.getTxAmount()).thenReturn(1);
        when(orderTx2.getEnergyProvider()).thenReturn(EnergyProviderName.ITRX);
        when(orderTx2.isAutodelegationOrder()).thenReturn(true);


        ArgumentCaptor<Long> captor1 = ArgumentCaptor.captor();
        ArgumentCaptor<Long> captor2 = ArgumentCaptor.captor();
        ReferralProgram referralProgram = mock50pcntWithSubtractAmountRp();

        Long commissionTx1 = calculator.calculateCommissionAsPercentFromProfit(orderTx1, referralProgram);
        verify(orderTx1).setRefProgramProfitRemainder(captor1.capture());

        Long commissionTx2 = calculator.calculateCommissionAsPercentFromProfit(orderTx2, referralProgram);
        verify(orderTx2).setRefProgramProfitRemainder(captor2.capture());

        Assertions.assertThat(commissionTx1).isEqualTo(962_500L);
        Assertions.assertThat(captor1.getValue()).isEqualTo(975_000L);

        Assertions.assertThat(commissionTx2).isEqualTo(925_000L);
        Assertions.assertThat(captor2.getValue()).isEqualTo(1_550_000L);
    }

    private ReferralProgram mock50pcntWOSubtractAmountRp() {
        ReferralProgram referralProgram = mock(ReferralProgram.class);
        when(referralProgram.getPercentage()).thenReturn(50);
        when(referralProgram.getSubtractAmountUseProviderAmount()).thenReturn(true);
        return referralProgram;
    }

    private ReferralProgram mock50pcntWithSubtractAmountRp() {
        ReferralProgram referralProgram = mock(ReferralProgram.class);
        when(referralProgram.getPercentage()).thenReturn(50);
        when(referralProgram.getSubtractAmountTx1Catfee()).thenReturn(AppConstants.BASE_SUBTRACT_AMOUNT_TX1);
        when(referralProgram.getSubtractAmountTx1Itrx()).thenReturn(AppConstants.BASE_SUBTRACT_AMOUNT_TX1);

        when(referralProgram.getSubtractAmountTx2Catfee()).thenReturn(AppConstants.BASE_SUBTRACT_AMOUNT_TX2);
        when(referralProgram.getSubtractAmountTx2Itrx()).thenReturn(AppConstants.BASE_SUBTRACT_AMOUNT_TX2);

        when(referralProgram.getSubtractAmountTx1AutoItrx()).thenReturn(AppConstants.BASE_SUBTRACT_AMOUNT_TX1_AUTO);
        when(referralProgram.getSubtractAmountTx2AutoItrx()).thenReturn(AppConstants.BASE_SUBTRACT_AMOUNT_TX2_AUTO);

        return referralProgram;
    }


    private ReferralProgram mockDefault5pcntRp() {
        ReferralProgram referralProgram = mock(ReferralProgram.class);
        when(referralProgram.getPercentage()).thenReturn(5);
        return referralProgram;
    }
  
}