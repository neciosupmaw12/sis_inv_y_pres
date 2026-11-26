import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';
import { Auth } from './core/services/auth';
import { Rol } from './core/models';
import { Login } from './pages/login/login';
import { Inicio } from './pages/inicio/inicio';
import { Equipos } from './pages/equipos/equipos';
import { Solicitantes } from './pages/solicitantes/solicitantes';
import { Prestamos } from './pages/prestamos/prestamos';
import { Usuarios } from './pages/usuarios/usuarios';
import { InicioAlumno } from './pages/alumno/inicio-alumno/inicio-alumno';
import { Catalogo } from './pages/alumno/catalogo/catalogo';
import { Solicitar } from './pages/alumno/solicitar/solicitar';
import { MisPrestamos } from './pages/alumno/mis-prestamos/mis-prestamos';
import { MiPerfil } from './pages/alumno/mi-perfil/mi-perfil';

/** Personal de la oficina de TI. */
const PERSONAL: Rol[] = ['ADMIN', 'ENCARGADO'];
const SOLO_ADMIN: Rol[] = ['ADMIN'];
const ALUMNO: Rol[] = ['ALUMNO'];

/** Cada ruta exige sesión (authGuard) y uno de los roles de data.roles (roleGuard); si no, va al inicio de su rol. */
const protegida = (roles: Rol[]) => ({ canActivate: [authGuard, roleGuard], data: { roles } });

export const routes: Routes = [
  { path: 'login', component: Login, title: 'Iniciar sesión' },

  // ADMIN y ENCARGADO
  { path: 'inicio', component: Inicio, title: 'Inicio', ...protegida(PERSONAL) },
  { path: 'equipos', component: Equipos, title: 'Inventario de equipos', ...protegida(PERSONAL) },
  { path: 'solicitantes', component: Solicitantes, title: 'Solicitantes', ...protegida(PERSONAL) },
  { path: 'prestamos', component: Prestamos, title: 'Préstamos', ...protegida(PERSONAL) },

  // Solo ADMIN
  { path: 'usuarios', component: Usuarios, title: 'Usuarios', ...protegida(SOLO_ADMIN) },

  // Solo ALUMNO
  { path: 'mi-inicio', component: InicioAlumno, title: 'Inicio', ...protegida(ALUMNO) },
  { path: 'catalogo', component: Catalogo, title: 'Catálogo de equipos', ...protegida(ALUMNO) },
  { path: 'solicitar', component: Solicitar, title: 'Solicitar préstamo', ...protegida(ALUMNO) },
  { path: 'mis-prestamos', component: MisPrestamos, title: 'Mis préstamos', ...protegida(ALUMNO) },
  { path: 'mi-perfil', component: MiPerfil, title: 'Mi perfil', ...protegida(ALUMNO) },

  // Sin ruta o ruta desconocida: al inicio del rol (o al login si no hay sesión)
  { path: '', pathMatch: 'full', redirectTo: () => inject(Auth).rutaInicio() },
  { path: '**', redirectTo: () => inject(Auth).rutaInicio() },
];
