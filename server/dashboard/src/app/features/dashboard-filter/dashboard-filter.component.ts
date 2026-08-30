import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Button } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Select } from 'primeng/select';

import { FilterOption, FiltersService } from '../../core/api';
import { DashboardFilterService } from './dashboard-filter.service';

@Component({
  selector: 'app-dashboard-filter',
  imports: [FormsModule, Button, DatePicker, Select],
  templateUrl: './dashboard-filter.html',
  styleUrl: './dashboard-filter.scss',
})
export class DashboardFilterComponent implements OnInit {
  private readonly filtersApi = inject(FiltersService);
  private readonly filterService = inject(DashboardFilterService);
  private readonly destroyRef = inject(DestroyRef);

  readonly users = signal<FilterOption[]>([]);
  readonly groups = signal<FilterOption[]>([]);
  readonly usersLoading = signal(true);
  readonly groupsLoading = signal(true);
  readonly usersError = signal(false);
  readonly groupsError = signal(false);
  readonly dateError = signal<string | null>(null);

  userId: number | null = null;
  groupId: number | null = null;
  dateFrom: Date | null = null;
  dateTo: Date | null = null;

  ngOnInit(): void {
    this.loadUsers();
    this.loadGroups();
  }

  apply(): void {
    if (this.dateFrom && this.dateTo && this.dateTo < this.dateFrom) {
      this.dateError.set('Дата окончания не может быть раньше даты начала.');
      return;
    }

    this.dateError.set(null);
    this.filterService.apply({
      userId: this.userId,
      groupId: this.groupId,
      dateFrom: this.toIsoDate(this.dateFrom),
      dateTo: this.toIsoDate(this.dateTo),
    });
  }

  reset(): void {
    this.userId = null;
    this.groupId = null;
    this.dateFrom = null;
    this.dateTo = null;
    this.dateError.set(null);
    this.filterService.reset();
  }

  private loadUsers(): void {
    this.usersLoading.set(true);
    this.filtersApi
      .getFilterUsers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (options) => this.users.set(options),
        error: () => {
          this.usersError.set(true);
          this.usersLoading.set(false);
        },
        complete: () => this.usersLoading.set(false),
      });
  }

  private loadGroups(): void {
    this.groupsLoading.set(true);
    this.filtersApi
      .getFilterGroups()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (options) => this.groups.set(options),
        error: () => {
          this.groupsError.set(true);
          this.groupsLoading.set(false);
        },
        complete: () => this.groupsLoading.set(false),
      });
  }

  private toIsoDate(value: Date | null): string | null {
    if (!value) {
      return null;
    }

    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
