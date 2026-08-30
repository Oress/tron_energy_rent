import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { OrderRow, OrdersService } from '../../core/api';
import { DashboardFilter } from '../dashboard-filter/dashboard-filter.model';
import { DashboardFilterService } from '../dashboard-filter/dashboard-filter.service';
import { RequestStateComponent } from '../../shared/components/request-state/request-state.component';
import {
  TablePageEvent,
  TablePaginatorComponent,
} from '../../shared/components/table-paginator/table-paginator.component';
import { TrxAmountPipe } from '../../shared/pipes/trx-amount.pipe';

@Component({
  selector: 'app-orders-table',
  imports: [DatePipe, DecimalPipe, RequestStateComponent, TablePaginatorComponent, TrxAmountPipe],
  templateUrl: './orders-table.html',
})
export class OrdersTableComponent {
  private readonly ordersApi = inject(OrdersService);
  private readonly filterService = inject(DashboardFilterService);
  private readonly destroyRef = inject(DestroyRef);
  private requestSequence = 0;

  readonly rows = signal<OrderRow[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly first = signal(0);
  readonly pageSize = signal(20);
  readonly total = signal(0);

  constructor() {
    effect(() => {
      this.filterService.revision();
      const filter = this.filterService.appliedFilter();
      this.first.set(0);
      this.loadPage(0, this.pageSize(), filter);
    });
  }

  onPage(event: TablePageEvent): void {
    const first = event.first;
    const size = event.rows;
    this.first.set(first);
    this.pageSize.set(size);
    this.loadPage(Math.floor(first / size), size, this.filterService.appliedFilter());
  }

  retry(): void {
    this.loadPage(
      Math.floor(this.first() / this.pageSize()),
      this.pageSize(),
      this.filterService.appliedFilter(),
    );
  }

  private loadPage(page: number, size: number, filter: DashboardFilter): void {
    const requestId = ++this.requestSequence;
    this.loading.set(true);
    this.error.set(null);

    this.ordersApi
      .getOrders(
        page,
        size,
        filter.userId ?? undefined,
        filter.groupId ?? undefined,
        filter.dateFrom ?? undefined,
        filter.dateTo ?? undefined,
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (requestId !== this.requestSequence) return;
          this.rows.set(response.content);
          this.total.set(response.totalElements);
        },
        error: () => {
          if (requestId !== this.requestSequence) return;
          this.rows.set([]);
          this.total.set(0);
          this.error.set('Проверьте соединение с сервером и повторите запрос.');
          this.loading.set(false);
        },
        complete: () => {
          if (requestId === this.requestSequence) this.loading.set(false);
        },
      });
  }
}
