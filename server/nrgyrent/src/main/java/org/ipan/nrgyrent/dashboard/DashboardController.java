package org.ipan.nrgyrent.dashboard;

import java.time.LocalDate;
import java.util.List;

import org.ipan.nrgyrent.dashboard.api.FiltersApi;
import org.ipan.nrgyrent.dashboard.api.OrdersApi;
import org.ipan.nrgyrent.dashboard.api.UserProfitApi;
import org.ipan.nrgyrent.dashboard.api.model.FilterOption;
import org.ipan.nrgyrent.dashboard.api.model.OrderPage;
import org.ipan.nrgyrent.dashboard.api.model.UserProfitPage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/** Implements the generated dashboard server contract. */
@RestController
@RequiredArgsConstructor
public class DashboardController implements FiltersApi, OrdersApi, UserProfitApi {
    private final DashboardQueryService dashboardQueryService;

    @Override
    public ResponseEntity<List<FilterOption>> getFilterGroups() {
        return ResponseEntity.ok(dashboardQueryService.getFilterGroups());
    }

    @Override
    public ResponseEntity<List<FilterOption>> getFilterUsers() {
        return ResponseEntity.ok(dashboardQueryService.getFilterUsers());
    }

    @Override
    public ResponseEntity<OrderPage> getOrders(Integer page, Integer size, Long userId, Long groupId,
                                               LocalDate dateFrom, LocalDate dateTo) {
        return ResponseEntity.ok(
                dashboardQueryService.getOrders(page, size, userId, groupId, dateFrom, dateTo));
    }

    @Override
    public ResponseEntity<UserProfitPage> getUserProfits(Integer page, Integer size, Long userId, Long groupId,
                                                         LocalDate dateFrom, LocalDate dateTo) {
        return ResponseEntity.ok(
                dashboardQueryService.getUserProfits(page, size, userId, groupId, dateFrom, dateTo));
    }
}
