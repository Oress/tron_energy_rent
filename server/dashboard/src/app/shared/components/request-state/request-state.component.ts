import { Component, input, output } from '@angular/core';
import { Button } from 'primeng/button';
import { ProgressSpinner } from 'primeng/progressspinner';

@Component({
  selector: 'app-request-state',
  imports: [Button, ProgressSpinner],
  templateUrl: './request-state.html',
})
export class RequestStateComponent {
  readonly loading = input(false);
  readonly error = input<string | null>(null);
  readonly empty = input(false);
  readonly emptyText = input('Нет данных по выбранным условиям.');
  readonly retry = output<void>();
}
