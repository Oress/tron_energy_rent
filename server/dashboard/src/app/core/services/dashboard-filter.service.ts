import { Injectable, signal } from '@angular/core';

/** Global dashboard filter — applied to every report table. */
export interface DashboardFilter {
  userId: number | null;
  groupId: number | null;
  /** Inclusive local date 'YYYY-MM-DD'. */
  dateFrom: string | null;
  /** Inclusive local date 'YYYY-MM-DD'. */
  dateTo: string | null;
}

export const EMPTY_FILTER: DashboardFilter = {
  userId: null,
  groupId: null,
  dateFrom: null,
  dateTo: null,
};

/**
 * Holds the global filter state shared by the filter bar and all report tables.
 * Tables react to {@link version} (incremented on every apply/reset) and read
 * the current values from {@link filter}.
 */
@Injectable({ providedIn: 'root' })
export class DashboardFilterService {
  private readonly filterSignal = signal<DashboardFilter>({ ...EMPTY_FILTER });
  private readonly versionSignal = signal(0);

  readonly filter = this.filterSignal.asReadonly();
  readonly version = this.versionSignal.asReadonly();

  apply(filter: DashboardFilter): void {
    this.filterSignal.set({ ...filter });
    this.versionSignal.update((v) => v + 1);
  }

  reset(): void {
    this.apply({ ...EMPTY_FILTER });
  }
}
