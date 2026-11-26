import { Component } from '@angular/core';

/** Pie de página de todas las pantallas (incluido el login). Se coloca en app.html, debajo del contenido. */
@Component({
  selector: 'app-footer',
  templateUrl: './footer.html',
  styleUrl: './footer.scss',
})
export class Footer {
  /** Se calcula al cargar la aplicación: no hay que cambiarlo a mano cada año. */
  protected readonly anio = new Date().getFullYear();
}
