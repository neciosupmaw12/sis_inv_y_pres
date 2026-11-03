import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Auth } from '../../core/services/auth';

interface Enlace {
  ruta: string;
  texto: string;
  icono: string;
}

@Component({
  selector: 'app-navbar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
})
export class Navbar {
  protected readonly auth = inject(Auth);
  protected readonly abierto = signal(false);

  protected readonly enlaces: Enlace[] = [
    { ruta: '/inicio', texto: 'Inicio', icono: 'bi-house' },
    { ruta: '/equipos', texto: 'Equipos', icono: 'bi-laptop' },
    { ruta: '/solicitantes', texto: 'Solicitantes', icono: 'bi-people' },
    { ruta: '/prestamos', texto: 'Préstamos', icono: 'bi-arrow-left-right' },
  ];
}
