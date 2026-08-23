package org.ipan.nrgyrent.dashboard;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * REST endpoints for the Angular dashboard report tables.
 * Contract: openapi/nrgyrent-api.yaml in the dashboard project.
 * <p>
 * All table endpoints accept the global dashboard filter as optional query params:
 * userId, groupId, dateFrom, dateTo (dates are inclusive, local day granularity).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    @GetMapping("/orders")
    public PageDto<OrderRowDto> getOrders(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) Long userId,
                                          @RequestParam(required = false) Long groupId,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                          @RequestParam(required = false)
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return dashboardQueryService.getOrdersPage(page, size, userId, groupId, dateFrom, dateTo);
    }

    @GetMapping("/profit-by-user")
    public PageDto<UserProfitRowDto> getUserProfit(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(required = false) Long userId,
                                                   @RequestParam(required = false) Long groupId,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                   @RequestParam(required = false)
                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return dashboardQueryService.getUserProfitPage(page, size, userId, groupId, dateFrom, dateTo);
    }

    @GetMapping("/deposits")
    public PageDto<DepositRowDto> getDeposits(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) Long groupId,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return dashboardQueryService.getDepositsPage(page, size, userId, groupId, dateFrom, dateTo);
    }

    @GetMapping("/referral-system")
    public PageDto<ReferralSystemRowDto> getReferralSystem(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size,
                                                           @RequestParam(required = false) Long userId,
                                                           @RequestParam(required = false) Long groupId,
                                                           @RequestParam(required = false)
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                           @RequestParam(required = false)
                                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return dashboardQueryService.getReferralSystemPage(page, size, userId, groupId, dateFrom, dateTo);
    }

    @GetMapping("/referral-payouts")
    public PageDto<ReferralPayoutRowDto> getReferralPayouts(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size,
                                                            @RequestParam(required = false) Long userId,
                                                            @RequestParam(required = false) Long groupId,
                                                            @RequestParam(required = false)
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                            @RequestParam(required = false)
                                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return dashboardQueryService.getReferralPayoutsPage(page, size, userId, groupId, dateFrom, dateTo);
    }

    @GetMapping("/withdrawals")
    public PageDto<WithdrawalRowDto> getWithdrawals(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(required = false) Long userId,
                                                    @RequestParam(required = false) Long groupId,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return dashboardQueryService.getWithdrawalsPage(page, size, userId, groupId, dateFrom, dateTo);
    }

    @GetMapping("/aml-checks")
    public PageDto<AmlCheckRowDto> getAmlChecks(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) Long groupId,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                @RequestParam(required = false)
                                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return dashboardQueryService.getAmlChecksPage(page, size, userId, groupId, dateFrom, dateTo);
    }

    @GetMapping("/statistics/overall")
    public OverallStatisticsDto getOverallStatistics(@RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                                     @RequestParam(required = false)
                                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return dashboardQueryService.getOverallStatistics(dateFrom, dateTo);
    }

    @GetMapping("/filter/users")
    public List<FilterUserDto> getFilterUsers() {
        return dashboardQueryService.getFilterUsers();
    }

    @GetMapping("/filter/groups")
    public List<FilterGroupDto> getFilterGroups() {
        return dashboardQueryService.getFilterGroups();
    }
}
