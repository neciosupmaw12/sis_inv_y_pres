import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { Login } from './pages/login/login';
import { Inicio } from './pages/inicio/inicio';
import { Equipos } from './pages/equipos/equipos';
import { Solicitantes } from './pages/solicitantes/solicitantes';
import { Prestamos } from './pages/prestamos/prestamos';
import { Usuarios } from './pages/usuarios/usuarios';

export const routes: Routes = [
  { path: 'login', component: Login, title: 'Iniciar sesión' },
  { path: 'inicio', component: Inicio, canActivate: [authGuard], title: 'Inicio' },
  { path: 'equipos', component: Equipos, canActivate: [authGuard], title: 'Inventario de equipos' },
  { path: 'solicitantes', component: Solicitantes, canActivate: [authGuard], title: 'Solicitantes' },
  { path: 'prestamos', component: Prestamos, canActivate: [authGuard], title: 'Préstamos' },
  {
    path: 'usuarios',
    component: Usuarios,
    canActivate: [authGuard],
    data: { roles: ['ADMIN'] },
    title: 'Usuarios',
  },
  { path: '', pathMatch: 'full', redirectTo: 'inicio' },
  { path: '**', redirectTo: 'inicio' },
];
