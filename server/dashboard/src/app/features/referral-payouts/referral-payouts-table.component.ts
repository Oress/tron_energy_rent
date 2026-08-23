import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import { Table } from 'primeng/table';

import { ReferralPayoutRowDto, ReferralService } from '../../core/api';
import { FormatUsdtPipe } from '../../shared/pipes/format-usdt.pipe';

/** PrimeNG page event payload (first = offset, rows = page size). */
interface PageEvent {
  first?: number;
  rows?: number;
}

/**
 * Таблица "Выплаты по реф программам" — server-side paginated via
 * GET /api/v1/referral-payouts.
 */
@Component({
  selector: 'app-referral-payouts-table',
  imports: [Table, DatePipe, FormatUsdtPipe],
  templateUrl: './referral-payouts-table.html',
  styleUrl: './referral-payouts-table.scss',
})
export class ReferralPayoutsTableComponent implements OnInit {
  private readonly referralService = inject(ReferralService);

  readonly rows = signal<ReferralPayoutRowDto[]>([]);
  readonly loading = signal(true);
  readonly first = signal(0);
  readonly pageSize = signal(20);
  readonly total = signal(0);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    const page = Math.floor(this.first() / this.pageSize());
    this.referralService.getReferralPayouts(page, this.pageSize()).subscribe({
      next: (res) => {
        this.rows.set(res.content ?? []);
        this.total.set(res.totalElements ?? 0);
      },
      error: () => {
        this.rows.set([]);
        this.total.set(0);
      },
      complete: () => this.loading.set(false),
    });
  }

  onPage(event: PageEvent): void {
    this.first.set(event.first ?? 0);
    this.pageSize.set(event.rows ?? this.pageSize());
    this.load();
  }
}
