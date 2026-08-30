import { Component, computed, input, output } from '@angular/core';

export interface TablePageEvent {
  first: number;
  rows: number;
}

@Component({
  selector: 'app-table-paginator',
  templateUrl: './table-paginator.html',
})
export class TablePaginatorComponent {
  readonly first = input.required<number>();
  readonly pageSize = input.required<number>();
  readonly total = input.required<number>();
  readonly disabled = input(false);
  readonly pageSizeOptions = input<readonly number[]>([10, 20, 50]);
  readonly page = output<TablePageEvent>();

  readonly currentPage = computed(() => Math.floor(this.first() / this.pageSize()) + 1);
  readonly pageCount = computed(() => Math.max(1, Math.ceil(this.total() / this.pageSize())));
  readonly rangeStart = computed(() => (this.total() === 0 ? 0 : this.first() + 1));
  readonly rangeEnd = computed(() => Math.min(this.first() + this.pageSize(), this.total()));
  readonly canGoBack = computed(() => this.first() > 0 && !this.disabled());
  readonly canGoForward = computed(
    () => this.first() + this.pageSize() < this.total() && !this.disabled(),
  );

  previous(): void {
    if (!this.canGoBack()) return;
    this.page.emit({ first: Math.max(0, this.first() - this.pageSize()), rows: this.pageSize() });
  }

  next(): void {
    if (!this.canGoForward()) return;
    this.page.emit({ first: this.first() + this.pageSize(), rows: this.pageSize() });
  }

  changePageSize(event: Event): void {
    const rows = Number((event.target as HTMLSelectElement).value);
    this.page.emit({ first: 0, rows });
  }
}
