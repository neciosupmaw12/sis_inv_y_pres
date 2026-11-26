import { Component, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { filter, interval, merge, startWith } from 'rxjs';
import { Auth } from '../../core/services/auth';
import { SolicitudesPendientes } from '../../core/services/solicitudes-pendientes';
import { CLASE_ROL, Rol } from '../../core/models';
import { EtiquetaPipe } from '../etiqueta-pipe';

interface Enlace {
  ruta: string;
  texto: string;
  icono: string;
  /** Muestra el contador de solicitudes pendientes. */
  contador?: boolean;
}

/** Opciones del menú de cada rol (el roleGuard y el backend impiden entrar a las demás). */
const ENLACES: Record<Rol, Enlace[]> = {
  ADMIN: [
    { ruta: '/inicio', texto: 'Inicio', icono: 'bi-house' },
    { ruta: '/equipos', texto: 'Equipos', icono: 'bi-laptop' },
    { ruta: '/solicitantes', texto: 'Solicitantes', icono: 'bi-people' },
    { ruta: '/prestamos', texto: 'Préstamos', icono: 'bi-arrow-left-right' },
    { ruta: '/solicitudes', texto: 'Solicitudes', icono: 'bi-inbox', contador: true },
    { ruta: '/usuarios', texto: 'Usuarios', icono: 'bi-person-gear' },
  ],
  ENCARGADO: [
    { ruta: '/inicio', texto: 'Inicio', icono: 'bi-house' },
    { ruta: '/equipos', texto: 'Equipos', icono: 'bi-laptop' },
    { ruta: '/solicitantes', texto: 'Solicitantes', icono: 'bi-people' },
    { ruta: '/prestamos', texto: 'Préstamos', icono: 'bi-arrow-left-right' },
  ],
  ALUMNO: [
    { ruta: '/mi-inicio', texto: 'Inicio', icono: 'bi-house' },
    { ruta: '/catalogo', texto: 'Catálogo', icono: 'bi-grid-3x3-gap' },
    { ruta: '/solicitar', texto: 'Solicitar préstamo', icono: 'bi-send' },
    { ruta: '/mis-prestamos', texto: 'Mis préstamos', icono: 'bi-journal-check' },
    { ruta: '/mi-perfil', texto: 'Mi perfil', icono: 'bi-person-circle' },
  ],
};

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, NgClass, EtiquetaPipe],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  protected readonly auth = inject(Auth);
  protected readonly pendientes = inject(SolicitudesPendientes);
  protected readonly abierto = signal(false);

  protected readonly enlaces = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? ENLACES[rol] : [];
  });
  protected readonly claseRol = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? CLASE_ROL[rol] : '';
  });

  constructor() {
    // Contador de solicitudes pendientes del ADMIN: al cargar, en cada navegación y cada minuto
    merge(inject(Router).events.pipe(filter((e) => e instanceof NavigationEnd)), interval(60_000))
      .pipe(
        startWith(null),
        filter(() => this.auth.esAdmin()),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.pendientes.actualizar());
  }
}
