package org.ipan.nrgyrent.telegram.utils;

public class SearchingUtils {

    public static String paramterContains(String val) {
        if (val == null) return null;

        return '%' + val + '%';
    }
}
