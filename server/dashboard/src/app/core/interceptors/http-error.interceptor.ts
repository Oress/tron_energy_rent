import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';

/** Keeps HTTP failures observable so each report can present and retry its own request. */
export const httpErrorInterceptor: HttpInterceptorFn = (request, next) =>
  next(request).pipe(
    catchError((error: unknown) =>
      throwError(() =>
        error instanceof HttpErrorResponse
          ? error
          : new HttpErrorResponse({ error, status: 0, statusText: 'Unknown client error' }),
      ),
    ),
  );
