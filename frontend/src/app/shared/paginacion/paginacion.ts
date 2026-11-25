import { Component, computed, input, output } from '@angular/core';
import { Pagina } from '../../core/models';

/** Un número de página o un separador de elipsis. */
type Item = number | '…';

/**
 * Controles de navegación entre páginas (Bootstrap 5.3):
 * Primera · Anterior · 1 … 4 5 6 … 20 · Siguiente · Última, selector de tamaño y texto "Mostrando 11–20 de 87".
 *
 * Uso: <app-paginacion [pagina]="pagina()" etiqueta="equipos" (paginaCambio)="irA($event)" (tamanoCambio)="cambiarTamano($event)" />
 */
@Component({
  selector: 'app-paginacion',
  templateUrl: './paginacion.html',
  styleUrl: './paginacion.scss',
})
export class Paginacion {
  readonly pagina = input.required<Pagina<unknown>>();
  /** Nombre de los registros en plural: "equipos", "préstamos"... */
  readonly etiqueta = input('registros');
  readonly tamanos = input<number[]>([5, 10, 20, 50]);
  /** Páginas visibles a cada lado de la actual antes de usar "…". */
  readonly vecinas = input(1);
  readonly deshabilitado = input(false);

  readonly paginaCambio = output<number>();
  readonly tamanoCambio = output<number>();

  protected readonly actual = computed(() => this.pagina().page);
  protected readonly total = computed(() => this.pagina().totalPages);
  protected readonly desde = computed(() =>
    this.pagina().totalElements === 0 ? 0 : this.pagina().page * this.pagina().size + 1,
  );
  protected readonly hasta = computed(() =>
    Math.min((this.pagina().page + 1) * this.pagina().size, this.pagina().totalElements),
  );

  /** Siempre la primera y la última página, la actual y sus vecinas; el resto se resume con "…". */
  protected readonly items = computed<Item[]>(() => {
    const total = this.total();
    const actual = this.actual();
    const v = this.vecinas();
    if (total <= 1) return total === 1 ? [0] : [];
    const visibles = new Set<number>([0, total - 1]);
    for (let i = actual - v; i <= actual + v; i++) {
      if (i >= 0 && i < total) visibles.add(i);
    }
    const ordenadas = [...visibles].sort((a, b) => a - b);
    const resultado: Item[] = [];
    ordenadas.forEach((n, i) => {
      const anterior = ordenadas[i - 1];
      if (i > 0 && n - anterior === 2) resultado.push(anterior + 1); // un solo hueco: se muestra el número
      else if (i > 0 && n - anterior > 2) resultado.push('…');
      resultado.push(n);
    });
    return resultado;
  });

  irA(n: number): void {
    if (this.deshabilitado() || n < 0 || n >= this.total() || n === this.actual()) return;
    this.paginaCambio.emit(n);
  }

  cambiarTamano(valor: string): void {
    this.tamanoCambio.emit(Number(valor));
  }
}
