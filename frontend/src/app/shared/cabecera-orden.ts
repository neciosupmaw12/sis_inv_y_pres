import { Component, computed, input, output } from '@angular/core';

/**
 * Cabecera de tabla ordenable. Al hacer clic alterna asc → desc y muestra el ícono del orden actual.
 * Uso: <th appOrden="nombre" [orden]="consulta().sort" (ordenar)="ordenar($event)">Equipo</th>
 */
@Component({
  selector: 'th[appOrden]',
  template: `
    <button type="button" class="btn-orden" [class.activo]="direccion()" [title]="titulo()" (click)="alternar()">
      <ng-content />
      <i class="bi" [class]="icono()"></i>
    </button>
  `,
  styles: `
    .btn-orden {
      all: unset;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 0.3rem;
      white-space: nowrap;
    }
    .btn-orden:focus-visible { outline: 2px solid var(--bs-primary); outline-offset: 2px; border-radius: 2px; }
    .btn-orden i { font-size: 0.8rem; opacity: 0.35; }
    .btn-orden.activo { color: var(--bs-primary); }
    .btn-orden.activo i { opacity: 1; }
  `,
  host: { '[attr.aria-sort]': 'ariaSort()' },
})
export class CabeceraOrden {
  /** Campo de la API por el que ordena esta columna. */
  readonly appOrden = input.required<string>();
  /** Orden actual del listado, por ejemplo "nombre,asc". */
  readonly orden = input.required<string>();
  /** Emite el nuevo orden: "campo,asc" o "campo,desc". */
  readonly ordenar = output<string>();

  protected readonly direccion = computed<'asc' | 'desc' | null>(() => {
    const [campo, dir] = this.orden().split(',');
    return campo === this.appOrden() ? (dir === 'desc' ? 'desc' : 'asc') : null;
  });
  protected readonly icono = computed(() =>
    this.direccion() === 'asc' ? 'bi-sort-up' : this.direccion() === 'desc' ? 'bi-sort-down' : 'bi-arrow-down-up',
  );
  protected readonly ariaSort = computed(() =>
    this.direccion() === 'asc' ? 'ascending' : this.direccion() === 'desc' ? 'descending' : 'none',
  );
  protected readonly titulo = computed(() =>
    this.direccion() === 'asc' ? 'Orden ascendente (clic para descendente)' : 'Ordenar por esta columna',
  );

  alternar(): void {
    this.ordenar.emit(`${this.appOrden()},${this.direccion() === 'asc' ? 'desc' : 'asc'}`);
  }
}
