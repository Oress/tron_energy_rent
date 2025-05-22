package org.ipan.nrgyrent.itrx;

import java.math.BigDecimal;

public class AppConstants {
    public static final String DURATION_1H = "1H";
    public static BigDecimal trxToSunRate = new BigDecimal(1_000_000);

    public static final int ENERGY_65K = 65_000;
    public static final int ENERGY_131K = 131_000;

    public static final Long PRICE_65K = 5_500_000L;
    public static final Long PRICE_131K = 8_550_000L;

    public static final Long WITHDRAWAL_FEE = 1_000_000L;

    public static final Long MIN_TRANSFER_AMOUNT_SUN = 10_000_000L;
    public static final Long MIN_WITHDRAWAL_AMOUNT = 10_000_000L;
}
