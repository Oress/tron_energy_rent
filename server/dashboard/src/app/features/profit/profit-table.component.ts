import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import { Table } from 'primeng/table';

import { ProfitService, UserProfitRowDto } from '../../core/api';
import { FormatUsdtPipe } from '../../shared/pipes/format-usdt.pipe';

/** PrimeNG page event payload (first = offset, rows = page size). */
interface PageEvent {
  first?: number;
  rows?: number;
}

/**
 * Таблица "Прибыль по пользователям" — server-side paginated via
 * GET /api/v1/profit-by-user.
 */
@Component({
  selector: 'app-profit-table',
  imports: [Table, DatePipe, FormatUsdtPipe],
  templateUrl: './profit-table.html',
  styleUrl: './profit-table.scss',
})
export class ProfitTableComponent implements OnInit {
  private readonly profitService = inject(ProfitService);

  readonly rows = signal<UserProfitRowDto[]>([]);
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
    this.profitService.getUserProfit(page, this.pageSize()).subscribe({
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
