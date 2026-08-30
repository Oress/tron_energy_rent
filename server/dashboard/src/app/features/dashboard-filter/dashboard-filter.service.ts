import { Injectable, signal } from '@angular/core';

import { DashboardFilter, EMPTY_DASHBOARD_FILTER } from './dashboard-filter.model';

@Injectable({ providedIn: 'root' })
export class DashboardFilterService {
  private readonly appliedFilterState = signal<DashboardFilter>({ ...EMPTY_DASHBOARD_FILTER });
  private readonly revisionState = signal(0);

  readonly appliedFilter = this.appliedFilterState.asReadonly();
  readonly revision = this.revisionState.asReadonly();

  apply(filter: DashboardFilter): void {
    this.appliedFilterState.set({ ...filter });
    this.revisionState.update((revision) => revision + 1);
  }

  reset(): void {
    this.apply(EMPTY_DASHBOARD_FILTER);
  }
}
