import { CanActivateFn } from '@angular/router';

/**
 * Navigation placeholder for the future server authentication flow.
 * A browser guard is never a security boundary; the server currently has no active authentication mechanism.
 */
export const authGuard: CanActivateFn = () => true;
