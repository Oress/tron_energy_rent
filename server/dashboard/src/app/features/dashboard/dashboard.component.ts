import { Component } from '@angular/core';

import { DashboardFilterComponent } from '../dashboard-filter/dashboard-filter.component';
import { DepositsTableComponent } from '../deposits/deposits-table.component';
import { GeneralStatisticsComponent } from '../general-statistics/general-statistics.component';
import { OrdersTableComponent } from '../orders/orders-table.component';
import { ReferralProgramPaymentsTableComponent } from '../referral-program-payments/referral-program-payments-table.component';
import { ReferralSystemTableComponent } from '../referral-system/referral-system-table.component';
import { UserProfitTableComponent } from '../user-profit/user-profit-table.component';

@Component({
  selector: 'app-dashboard',
  imports: [
    DashboardFilterComponent,
    OrdersTableComponent,
    UserProfitTableComponent,
    DepositsTableComponent,
    GeneralStatisticsComponent,
    ReferralSystemTableComponent,
    ReferralProgramPaymentsTableComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent {}
