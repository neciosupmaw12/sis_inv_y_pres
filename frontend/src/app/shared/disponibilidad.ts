import { Directive, computed, input } from '@angular/core';

/**
 * Directiva de atributo: resalta las unidades disponibles de un equipo.
 * Uso: <span [appDisponibilidad]="equipo.cantidadDisponible" [limite]="2">...</span>
 */
@Directive({
  selector: '[appDisponibilidad]',
  host: {
    '[class.text-danger]': 'agotado()',
    '[class.fw-bold]': 'bajo()',
    '[class.text-warning-emphasis]': 'bajo() && !agotado()',
    '[attr.title]': 'mensaje()',
  },
})
export class Disponibilidad {
  readonly appDisponibilidad = input.required<number>();
  readonly limite = input(2);

  protected readonly agotado = computed(() => this.appDisponibilidad() === 0);
  protected readonly bajo = computed(() => this.appDisponibilidad() <= this.limite());
  protected readonly mensaje = computed(() =>
    this.agotado()
      ? 'Sin unidades disponibles'
      : this.bajo()
        ? 'Pocas unidades disponibles'
        : 'Unidades suficientes',
  );
}
