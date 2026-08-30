package org.ipan.nrgyrent.dashboard;

import java.time.LocalDate;
import java.util.List;

import org.ipan.nrgyrent.dashboard.api.model.FilterOption;
import org.ipan.nrgyrent.dashboard.api.model.DepositPage;
import org.ipan.nrgyrent.dashboard.api.model.GeneralStatistics;
import org.ipan.nrgyrent.dashboard.api.model.OrderPage;
import org.ipan.nrgyrent.dashboard.api.model.UserProfitPage;
import org.springframework.stereotype.Service;

/**
 * Plain-SQL seam for dashboard reports.
 *
 * Query implementations intentionally remain empty in this foundation increment.
 */
@Service
public class DashboardQueryService {

    public OrderPage getOrders(Integer page, Integer size, Long userId, Long groupId,
                               LocalDate dateFrom, LocalDate dateTo) {
        //TODO: implement queries
        return DashboardPageFactory.emptyOrders(page, size);
    }

    public UserProfitPage getUserProfits(Integer page, Integer size, Long userId, Long groupId,
                                         LocalDate dateFrom, LocalDate dateTo) {
        //TODO: implement queries
        return DashboardPageFactory.emptyUserProfits(page, size);
    }

    public DepositPage getDeposits(Integer page, Integer size, Long userId, Long groupId,
                                   LocalDate dateFrom, LocalDate dateTo) {
        //TODO: implement queries
        return DashboardPageFactory.emptyDeposits(page, size);
    }

    public GeneralStatistics getGeneralStatistics(Long userId, Long groupId,
                                                  LocalDate dateFrom, LocalDate dateTo) {
        //TODO: implement queries
        return DashboardPageFactory.emptyGeneralStatistics();
    }

    public List<FilterOption> getFilterUsers() {
        //TODO: implement queries
        return List.of();
    }

    public List<FilterOption> getFilterGroups() {
        //TODO: implement queries
        return List.of();
    }
}
