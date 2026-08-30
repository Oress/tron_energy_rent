import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'usdtAmount' })
export class UsdtAmountPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined || !Number.isFinite(value)) {
      return '—';
    }

    return `${value.toLocaleString('ru-RU', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 6,
    })} USDT`;
  }
}
