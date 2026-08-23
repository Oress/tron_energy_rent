import { Component } from '@angular/core';

import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { OverallStatisticsComponent } from '../statistics/overall-statistics.component';
import { OrdersTableComponent } from '../orders/orders-table.component';
import { DepositsTableComponent } from '../deposits/deposits-table.component';
import { ProfitTableComponent } from '../profit/profit-table.component';
import { ReferralSystemTableComponent } from '../referral-system/referral-system-table.component';
import { ReferralPayoutsTableComponent } from '../referral-payouts/referral-payouts-table.component';
import { WithdrawalsTableComponent } from '../withdrawals/withdrawals-table.component';
import { AmlChecksTableComponent } from '../aml-checks/aml-checks-table.component';
import { buildTime } from '../../../environments/version';

/**
 * The dashboard is a sequence of report sections: overall statistics block
 * followed by the paginated report tables.
 */
@Component({
  selector: 'app-dashboard',
  imports: [
    PageHeaderComponent,
    OverallStatisticsComponent,
    OrdersTableComponent,
    DepositsTableComponent,
    ProfitTableComponent,
    ReferralSystemTableComponent,
    ReferralPayoutsTableComponent,
    WithdrawalsTableComponent,
    AmlChecksTableComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent {
  readonly buildTime = buildTime;
}
