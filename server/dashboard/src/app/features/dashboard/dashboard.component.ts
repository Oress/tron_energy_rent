import { Component } from '@angular/core';

import { DashboardFilterComponent } from '../dashboard-filter/dashboard-filter.component';
import { OrdersTableComponent } from '../orders/orders-table.component';
import { UserProfitTableComponent } from '../user-profit/user-profit-table.component';

@Component({
  selector: 'app-dashboard',
  imports: [DashboardFilterComponent, OrdersTableComponent, UserProfitTableComponent],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardComponent {}
