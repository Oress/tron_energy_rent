import { Component, computed, inject, OnInit, signal } from '@angular/core';

import { OverallStatisticsDto, StatisticsService } from '../../core/api';
import { FormatUsdtPipe } from '../../shared/pipes/format-usdt.pipe';

interface StatItem {
  label: string;
  value: string | number | null | undefined;
}

/**
 * Блок "Общая статистика" — label/value metrics from GET /api/v1/statistics/overall,
 * rendered as a two-column list on desktop, single column on phones.
 */
@Component({
  selector: 'app-overall-statistics',
  imports: [FormatUsdtPipe],
  templateUrl: './overall-statistics.html',
  styleUrl: './overall-statistics.scss',
})
export class OverallStatisticsComponent implements OnInit {
  private readonly statisticsService = inject(StatisticsService);

  readonly data = signal<OverallStatisticsDto | null>(null);
  readonly loading = signal(true);

  readonly items = computed<StatItem[]>(() => {
    const d = this.data();
    return [
      { label: 'Прибыль (за период)', value: d?.profitPeriod },
      { label: 'Баланс Bybit', value: d?.bybitBalance },
      { label: 'AML прибыль (за период)', value: d?.amlProfitPeriod },
      { label: 'Реф. выплаты в ожидании', value: d?.pendingReferralPayouts },
      { label: 'Баланс itrx.io', value: d?.itrxBalance },
      { label: 'Баланс catfee.io', value: d?.catfeeBalance },
      { label: 'Баланс trxx.io', value: d?.trxxBalance },
      { label: 'Баланс netts.io', value: d?.nettsBalance },
      { label: 'Баланс свип кошельков', value: d?.sweepWalletsBalance },
      { label: 'Сумма балансов юзеров', value: d?.usersBalanceSum },
      { label: 'Реф. выплаты (за период)', value: d?.referralPayoutsPeriod },
      { label: 'Остаток от реф. программ (за период)', value: d?.referralProgramsRemainder },
      { label: 'Сумма депозитов', value: d?.depositsSum },
      { label: 'Комиссия поставщиков (период)', value: d?.providersCommissionPeriod },
      { label: 'Новое Поле', value: d?.newField },
    ];
  });

  ngOnInit(): void {
    this.statisticsService.getOverallStatistics().subscribe({
      next: (res) => this.data.set(res),
      error: () => this.data.set(null),
      complete: () => this.loading.set(false),
    });
  }
}
