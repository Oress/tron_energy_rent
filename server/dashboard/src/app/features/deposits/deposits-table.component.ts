import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';

import { Table } from 'primeng/table';

import { DepositRowDto, DepositService } from '../../core/api';
import { FormatUsdtPipe } from '../../shared/pipes/format-usdt.pipe';

/** PrimeNG page event payload (first = offset, rows = page size). */
interface PageEvent {
  first?: number;
  rows?: number;
}

/**
 * Таблица "Депозиты" — server-side paginated via GET /api/v1/deposits.
 */
@Component({
  selector: 'app-deposits-table',
  imports: [Table, DatePipe, FormatUsdtPipe],
  templateUrl: './deposits-table.html',
  styleUrl: './deposits-table.scss',
})
export class DepositsTableComponent implements OnInit {
  private readonly depositService = inject(DepositService);

  readonly rows = signal<DepositRowDto[]>([]);
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
    this.depositService.getDeposits(page, this.pageSize()).subscribe({
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
