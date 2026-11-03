import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/auth';
import { Rol } from '../models';

/** Exige sesión iniciada y, si la ruta define data.roles, que el usuario tenga uno de ellos. */
export const authGuard: CanActivateFn = (route) => {
  const auth = inject(Auth);
  const router = inject(Router);

  if (!auth.autenticado()) {
    return router.createUrlTree(['/login']);
  }
  const roles = route.data['roles'] as Rol[] | undefined;
  if (roles && !auth.tieneRol(roles)) {
    return router.createUrlTree(['/inicio']);
  }
  return true;
};
