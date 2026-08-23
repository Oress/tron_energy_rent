import { Pipe, PipeTransform } from '@angular/core';

/**
 * Formats a numeric amount with thousands separators and an optional unit suffix,
 * e.g. (1234.5) -> "1,234.50 USDT" (unit defaults to "USDT").
 * Returns an em dash for empty input.
 */
@Pipe({ name: 'formatUsdt' })
export class FormatUsdtPipe implements PipeTransform {
  transform(value: number | string | null | undefined, digits = 2, unit = 'USDT'): string {
    if (value === null || value === undefined || value === '') {
      return '—';
    }
    const num = typeof value === 'string' ? Number(value) : value;
    if (!Number.isFinite(num)) {
      return '—';
    }
    const formatted = num.toLocaleString('en-US', {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits,
    });
    return unit ? `${formatted} ${unit}` : formatted;
  }
}
