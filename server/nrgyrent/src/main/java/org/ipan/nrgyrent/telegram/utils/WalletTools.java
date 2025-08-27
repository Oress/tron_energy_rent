package org.ipan.nrgyrent.telegram.utils;

import java.util.regex.Pattern;

public class WalletTools {
    public static final Pattern TRON_ADDRESS_PATTERN = Pattern.compile("^[T]([123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]*)$");

    public static boolean isValidTronAddress(String address) {
        return TRON_ADDRESS_PATTERN.matcher(address).matches();
    }

    public static String formatTronAddressMd(String address) {
        if (address == null || address.length() < 6) {
            return address; // Return the original address if it's null or too short
        }

        String firstPart = address.substring(0, 8);
        String lastPart = address.substring(address.length() - 6);
        String middlePart = "\\*\\*\\*\\*";

        return firstPart + middlePart + lastPart;
    }


    public static String formatTronAddress(String address) {
        if (address == null || address.length() < 6) {
            return address; // Return the original address if it's null or too short
        }

        String firstPart = address.substring(0, 8);
        String lastPart = address.substring(address.length() - 6);
        String middlePart = "****";

        return firstPart + middlePart + lastPart;
    }

    public static String formatTronAddressAndLabel(String address, String label) {
        String formattedAddress = formatTronAddress(address);
        if (label != null && !label.isEmpty()) {
            formattedAddress = formattedAddress + " (" + label + ")";
        }
        return formattedAddress;
    }

    public static String formatTronAddressSuffixOnly(String address) {
        if (address == null || address.length() < 6) {
            return address; // Return the original address if it's null or too short
        }

        String lastPart = address.substring(address.length() - 6);
        return "****" + lastPart;
    }
}
