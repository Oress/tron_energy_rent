export interface DashboardFilter {
  userId: number | null;
  groupId: number | null;
  dateFrom: string | null;
  dateTo: string | null;
}

export const EMPTY_DASHBOARD_FILTER: DashboardFilter = {
  userId: null,
  groupId: null,
  dateFrom: null,
  dateTo: null,
};
