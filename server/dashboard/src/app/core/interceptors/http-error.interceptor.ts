import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { MessageService } from 'primeng/api';

/**
 * Central HTTP error handling: surfaces every failed request as a PrimeNG toast
 * (rendered by the <p-toast> in the layout) and re-throws so callers can react.
 *
 * TODO: on 401 redirect to a login route once the server exposes dashboard auth.
 */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const messageService = inject(MessageService);

  return next(req).pipe(
    catchError((error) => {
      const status = typeof error?.status === 'number' ? error.status : 'network';
      messageService.add({
        severity: 'error',
        summary: `Request failed (${status})`,
        detail: `${req.method} ${req.urlWithParams}`,
        life: 5000,
      });
      return throwError(() => error);
    }),
  );
};
