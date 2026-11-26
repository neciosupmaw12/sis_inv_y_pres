import { Component, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../core/services/auth';
import { CLASE_ROL, Rol } from '../../core/models';
import { EtiquetaPipe } from '../etiqueta-pipe';

interface Enlace {
  ruta: string;
  texto: string;
  icono: string;
}

/** Opciones del menú de cada rol (el roleGuard y el backend impiden entrar a las demás). */
const ENLACES: Record<Rol, Enlace[]> = {
  ADMIN: [
    { ruta: '/inicio', texto: 'Inicio', icono: 'bi-house' },
    { ruta: '/equipos', texto: 'Equipos', icono: 'bi-laptop' },
    { ruta: '/solicitantes', texto: 'Solicitantes', icono: 'bi-people' },
    { ruta: '/prestamos', texto: 'Préstamos', icono: 'bi-arrow-left-right' },
    { ruta: '/usuarios', texto: 'Usuarios', icono: 'bi-person-gear' },
  ],
  ENCARGADO: [
    { ruta: '/inicio', texto: 'Inicio', icono: 'bi-house' },
    { ruta: '/equipos', texto: 'Equipos', icono: 'bi-laptop' },
    { ruta: '/solicitantes', texto: 'Solicitantes', icono: 'bi-people' },
    { ruta: '/prestamos', texto: 'Préstamos', icono: 'bi-arrow-left-right' },
  ],
  ALUMNO: [],
};

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive, NgClass, EtiquetaPipe],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  protected readonly auth = inject(Auth);
  protected readonly abierto = signal(false);

  protected readonly enlaces = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? ENLACES[rol] : [];
  });
  protected readonly claseRol = computed(() => {
    const rol = this.auth.usuario()?.rol;
    return rol ? CLASE_ROL[rol] : '';
  });
}
