import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { GeneralStatistics, GeneralStatisticsService } from '../../core/api';
import { RequestStateComponent } from '../../shared/components/request-state/request-state.component';
import { TrxAmountPipe } from '../../shared/pipes/trx-amount.pipe';
import { DashboardFilter } from '../dashboard-filter/dashboard-filter.model';
import { DashboardFilterService } from '../dashboard-filter/dashboard-filter.service';

interface StatisticMetric {
  label: string;
  value: number;
}

@Component({
  selector: 'app-general-statistics',
  imports: [RequestStateComponent, TrxAmountPipe],
  templateUrl: './general-statistics.html',
})
export class GeneralStatisticsComponent {
  private readonly statisticsApi = inject(GeneralStatisticsService);
  private readonly filterService = inject(DashboardFilterService);
  private readonly destroyRef = inject(DestroyRef);
  private requestSequence = 0;

  readonly statistics = signal<GeneralStatistics | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly metrics = computed<StatisticMetric[]>(() => {
    const statistics = this.statistics();
    if (!statistics) return [];

    return [
      { label: 'Прибыль (за период)', value: statistics.periodProfit },
      { label: 'Баланс Bybit', value: statistics.bybitBalance },
      { label: 'AML прибыль (за период)', value: statistics.periodAmlProfit },
      { label: 'Реф. выплаты в ожидании', value: statistics.pendingReferralPayments },
      { label: 'Баланс itrx.io', value: statistics.itrxBalance },
      { label: 'Баланс catfee.io', value: statistics.catfeeBalance },
      { label: 'Баланс trxx.io', value: statistics.trxxBalance },
      { label: 'Баланс netts.io', value: statistics.nettsBalance },
      { label: 'Баланс свип кошельков', value: statistics.sweepWalletsBalance },
      { label: 'Сумма балансов юзеров', value: statistics.userBalancesTotal },
      { label: 'Реф. выплаты (за период)', value: statistics.periodReferralPayments },
      {
        label: 'Остаток от реф. программ (за период)',
        value: statistics.periodReferralProgramRemainder,
      },
      { label: 'Сумма депозитов', value: statistics.depositsTotal },
      { label: 'Комиссия поставщиков (период)', value: statistics.periodProviderCommission },
      { label: 'Новое Поле', value: statistics.newField },
    ];
  });

  constructor() {
    effect(() => {
      this.filterService.revision();
      this.load(this.filterService.appliedFilter());
    });
  }

  retry(): void {
    this.load(this.filterService.appliedFilter());
  }

  private load(filter: DashboardFilter): void {
    const requestId = ++this.requestSequence;
    this.loading.set(true);
    this.error.set(null);

    this.statisticsApi
      .getGeneralStatistics(
        filter.userId ?? undefined,
        filter.groupId ?? undefined,
        filter.dateFrom ?? undefined,
        filter.dateTo ?? undefined,
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (requestId !== this.requestSequence) return;
          this.statistics.set(response);
        },
        error: () => {
          if (requestId !== this.requestSequence) return;
          this.statistics.set(null);
          this.error.set('Проверьте соединение с сервером и повторите запрос.');
          this.loading.set(false);
        },
        complete: () => {
          if (requestId === this.requestSequence) this.loading.set(false);
        },
      });
  }
}
