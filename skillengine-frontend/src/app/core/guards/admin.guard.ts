import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // Not logged in → login page
  if (!auth.isAuthenticated()) {
    return router.createUrlTree(['/login']);
  }

  // Logged in but not admin → explorer
  if (!auth.isAdmin()) {
    return router.createUrlTree(['/explorer']);
  }

  // Admin → allow access
  return true;
};