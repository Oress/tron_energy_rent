package org.ipan.nrgyrent.itrx;

import java.math.BigDecimal;

public class AppConstants {
    public static final BigDecimal HUNDRED = new BigDecimal(100);

    public static final String PROVIDER_CATFEE = "CATFEE";
    public static final String PROVIDER_ITRX = "ITRX";

    public static final String CONFIG_ENERGY_PROVIDER = "ENERGY_PROVIDER";

    public static final Long BASE_SUBTRACT_AMOUNT_TX1 = 3_175_000L;
    public static final Long BASE_SUBTRACT_AMOUNT_TX2 = 6_350_000L;

    public static final Long DEFAULT_TARIFF_ID = 1L;

    public static final Long HOUR_MILLIS = 60L * 60 * 1000;
    public static final String DURATION_1H = "1H";
    public static BigDecimal trxToSunRate = new BigDecimal(1_000_000);

    public static final int ENERGY_65K = 65_000;
    public static final int ENERGY_131K = 131_000;

    public static final Long PRICE_65K = 5_500_000L;
    public static final Long PRICE_131K = 8_600_000L;

    public static final Long WITHDRAWAL_FEE = 1_000_000L;

    public static final Long MIN_TRANSFER_AMOUNT_USDT = 10_000_000L; // 10 USDT

    public static final Long MIN_TRANSFER_AMOUNT_SUN = 10_000_000L;
    public static final Long MIN_WITHDRAWAL_AMOUNT = 10_000_000L;
}
