import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

/**
 * Route guard for the authenticated dashboard area.
 *
 * TODO: replace the hard-coded check with a real AuthService once the server
 * exposes authentication for the dashboard (e.g. a session token endpoint).
 */
export const authGuard: CanActivateFn = () => {
  const router = inject(Router);
  const authenticated = true;

  if (!authenticated) {
    return router.createUrlTree(['/login']);
  }
  return true;
};
