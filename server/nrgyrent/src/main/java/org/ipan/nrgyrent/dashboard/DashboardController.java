package org.ipan.nrgyrent.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * REST endpoints for the Angular dashboard report tables.
 * Contract: openapi/nrgyrent-api.yaml in the dashboard project.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    @GetMapping("/orders")
    public PageDto<OrderRowDto> getOrders(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return dashboardQueryService.getOrdersPage(page, size);
    }

    @GetMapping("/profit-by-user")
    public PageDto<UserProfitRowDto> getUserProfit(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return dashboardQueryService.getUserProfitPage(page, size);
    }

    @GetMapping("/deposits")
    public PageDto<DepositRowDto> getDeposits(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return dashboardQueryService.getDepositsPage(page, size);
    }

    @GetMapping("/statistics/overall")
    public OverallStatisticsDto getOverallStatistics() {
        return dashboardQueryService.getOverallStatistics();
    }

    @GetMapping("/referral-system")
    public PageDto<ReferralSystemRowDto> getReferralSystem(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return dashboardQueryService.getReferralSystemPage(page, size);
    }

    @GetMapping("/referral-payouts")
    public PageDto<ReferralPayoutRowDto> getReferralPayouts(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        return dashboardQueryService.getReferralPayoutsPage(page, size);
    }

    @GetMapping("/withdrawals")
    public PageDto<WithdrawalRowDto> getWithdrawals(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return dashboardQueryService.getWithdrawalsPage(page, size);
    }

    @GetMapping("/aml-checks")
    public PageDto<AmlCheckRowDto> getAmlChecks(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return dashboardQueryService.getAmlChecksPage(page, size);
    }
}
