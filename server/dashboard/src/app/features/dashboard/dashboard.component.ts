import { Component } from '@angular/core';

import { DashboardFilterComponent } from '../dashboard-filter/dashboard-filter.component';
import { DepositsTableComponent } from '../deposits/deposits-table.component';
import { GeneralStatisticsComponent } from '../general-statistics/general-statistics.component';
import { OrdersTableComponent } from '../orders/orders-table.component';
import { UserProfitTableComponent } from '../user-profit/user-profit-table.component';

@Component({
  selector: 'app-dashboard',
  imports: [
    DashboardFilterComponent,
    OrdersTableComponent,
    UserProfitTableComponent,
    DepositsTableComponent,
    GeneralStatisticsComponent,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent {}
