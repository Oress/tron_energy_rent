import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-request-state',
  templateUrl: './request-state.html',
})
export class RequestStateComponent {
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly empty = input(false);
  readonly emptyText = input('Нет данных по выбранным условиям.');
  readonly retry = output<void>();
}
