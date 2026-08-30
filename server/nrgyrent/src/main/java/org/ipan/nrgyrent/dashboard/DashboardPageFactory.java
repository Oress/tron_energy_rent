package org.ipan.nrgyrent.dashboard;

import java.util.List;

import org.ipan.nrgyrent.dashboard.api.model.OrderPage;
import org.ipan.nrgyrent.dashboard.api.model.UserProfitPage;

/** Creates generated page models with stable pagination bounds. */
public final class DashboardPageFactory {
    public static final int DEFAULT_SIZE = 20;
    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 200;

    private DashboardPageFactory() {
    }

    public static int normalizePage(Integer page) {
        return page == null ? 0 : Math.max(page, 0);
    }

    public static int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.clamp(size, MIN_SIZE, MAX_SIZE);
    }

    public static OrderPage emptyOrders(Integer page, Integer size) {
        return new OrderPage(List.of(), normalizePage(page), normalizeSize(size), 0L, 0);
    }

    public static UserProfitPage emptyUserProfits(Integer page, Integer size) {
        return new UserProfitPage(List.of(), normalizePage(page), normalizeSize(size), 0L, 0);
    }
}
