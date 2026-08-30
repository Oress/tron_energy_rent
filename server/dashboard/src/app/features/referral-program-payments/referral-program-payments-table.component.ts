import { DatePipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
  ReferralProgramPaymentRow,
  ReferralProgramPaymentsService,
} from '../../core/api';
import { RequestStateComponent } from '../../shared/components/request-state/request-state.component';
import {
  TablePageEvent,
  TablePaginatorComponent,
} from '../../shared/components/table-paginator/table-paginator.component';
import { TrxAmountPipe } from '../../shared/pipes/trx-amount.pipe';
import { DashboardFilter } from '../dashboard-filter/dashboard-filter.model';
import { DashboardFilterService } from '../dashboard-filter/dashboard-filter.service';

@Component({
  selector: 'app-referral-program-payments-table',
  imports: [DatePipe, RequestStateComponent, TablePaginatorComponent, TrxAmountPipe],
  templateUrl: './referral-program-payments-table.html',
})
export class ReferralProgramPaymentsTableComponent {
  private readonly paymentsApi = inject(ReferralProgramPaymentsService);
  private readonly filterService = inject(DashboardFilterService);
  private readonly destroyRef = inject(DestroyRef);
  private requestSequence = 0;

  readonly rows = signal<ReferralProgramPaymentRow[]>([]);
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
    this.first.set(event.first);
    this.pageSize.set(event.rows);
    this.loadPage(
      Math.floor(event.first / event.rows),
      event.rows,
      this.filterService.appliedFilter(),
    );
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

    this.paymentsApi
      .getReferralProgramPayments(
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
