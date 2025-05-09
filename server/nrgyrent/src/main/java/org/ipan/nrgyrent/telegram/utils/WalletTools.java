package org.ipan.nrgyrent.telegram.utils;

import java.util.regex.Pattern;

public class WalletTools {
    public static final Pattern TRON_ADDRESS_PATTERN = Pattern.compile("^[T]([123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]*)$");

    public static boolean isValidTronAddress(String address) {
        return TRON_ADDRESS_PATTERN.matcher(address).matches();
    }
}
