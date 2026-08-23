import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';

import { FilterGroupDto, FilterService, FilterUserDto } from '../../core/api';
import { DashboardFilterService } from '../../core/services/dashboard-filter.service';

/**
 * Global filter bar: user selector, group selector, date range.
 * On "Применить" the filter is pushed into DashboardFilterService and every
 * report table reloads with the new parameters.
 */
@Component({
  selector: 'app-dashboard-filter',
  imports: [FormsModule, Button, Select, DatePicker],
  templateUrl: './dashboard-filter.html',
  styleUrl: './dashboard-filter.scss',
})
export class DashboardFilterComponent implements OnInit {
  private readonly filterService = inject(DashboardFilterService);
  private readonly api = inject(FilterService);

  readonly users = signal<FilterUserDto[]>([]);
  readonly groups = signal<FilterGroupDto[]>([]);

  /** Local (not yet applied) form state. */
  protected userId: number | null = null;
  protected groupId: number | null = null;
  protected dateFrom: Date | null = null;
  protected dateTo: Date | null = null;

  ngOnInit(): void {
    this.api.getFilterUsers().subscribe({
      next: (users) => this.users.set(users ?? []),
      error: () => this.users.set([]),
    });
    this.api.getFilterGroups().subscribe({
      next: (groups) => this.groups.set(groups ?? []),
      error: () => this.groups.set([]),
    });
  }

  apply(): void {
    this.filterService.apply({
      userId: this.userId ?? null,
      groupId: this.groupId ?? null,
      dateFrom: this.toDateString(this.dateFrom),
      dateTo: this.toDateString(this.dateTo),
    });
  }

  reset(): void {
    this.userId = null;
    this.groupId = null;
    this.dateFrom = null;
    this.dateTo = null;
    this.filterService.reset();
  }

  /** Formats a Date as a local 'YYYY-MM-DD' string (no UTC conversion). */
  private toDateString(date: Date | null): string | null {
    if (!date) {
      return null;
    }
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }
}
