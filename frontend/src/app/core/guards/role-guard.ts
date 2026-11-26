import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Auth } from '../services/auth';
import { Rol } from '../models';

/**
 * Permite entrar solo a los roles de {@code data.roles}. Si un alumno escribe /usuarios (o un ADMIN /mis-prestamos)
 * en la URL, se le redirige a la pantalla de inicio de su rol.
 * <p>
 * Es solo para la navegación: la seguridad real está en el backend (SecurityConfig y @PreAuthorize responden 403).
 * Uso: {@code canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] }}
 */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(Auth);
  const roles = route.data['roles'] as Rol[] | undefined;
  if (!roles || auth.tieneRol(roles)) {
    return true;
  }
  return inject(Router).createUrlTree([auth.rutaInicio()]);
};
