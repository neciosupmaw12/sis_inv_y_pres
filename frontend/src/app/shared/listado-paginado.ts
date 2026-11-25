import { DestroyRef, inject, signal, WritableSignal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, ParamMap, Params, Router } from '@angular/router';
import {
  Observable,
  Subject,
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  map,
  of,
  startWith,
  switchMap,
  tap,
} from 'rxjs';
import { ConsultaPaginada, Pagina } from '../core/models';
import { leerError } from '../core/services/http-error';

export const TAMANOS_PAGINA = [5, 10, 20, 50];

/**
 * Estado de un listado paginado guardado en la URL (?pagina=2&tamano=10&orden=nombre,asc&q=lap&estado=...),
 * para que no se pierda al recargar la página ni al usar Atrás / Adelante del navegador.
 *
 * Flujo RxJS:
 *  - Buscador: valueChanges → debounceTime(400) → distinctUntilChanged → actualiza la URL.
 *  - URL (queryParamMap) + recargas manuales → switchMap → GET paginado. switchMap cancela la petición anterior
 *    si el usuario sigue escribiendo o cambia de página antes de recibir la respuesta.
 *
 * Debe crearse en el inicializador de un campo del componente (usa inject()).
 */
export class ListadoPaginado<F extends ConsultaPaginada, T> {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly recargar$ = new Subject<void>();

  readonly consulta: WritableSignal<F>;
  readonly pagina = signal<Pagina<T> | null>(null);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly busqueda = new FormControl('', { nonNullable: true });

  /**
   * @param defectos valores por defecto de la consulta (page, size, sort, busqueda y filtros)
   * @param filtros  nombres de los filtros adicionales que se guardan en la URL (por ejemplo, ['estado'])
   * @param cargar   función que pide la página al backend
   */
  constructor(
    private readonly defectos: F,
    private readonly filtros: (keyof F & string)[],
    private readonly cargar: (consulta: F) => Observable<Pagina<T>>,
  ) {
    this.consulta = signal<F>(defectos);

    this.busqueda.valueChanges
      .pipe(
        debounceTime(400),
        map((texto) => texto.trim()),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((texto) => {
        if (texto !== (this.consulta().busqueda ?? '')) this.navegar({ busqueda: texto, page: 0 } as Partial<F>);
      });

    combineLatest([this.route.queryParamMap, this.recargar$.pipe(startWith(undefined))])
      .pipe(
        map(([params]) => this.leer(params)),
        tap((consulta) => {
          this.consulta.set(consulta);
          if (this.busqueda.value.trim() !== (consulta.busqueda ?? '')) {
            this.busqueda.setValue(consulta.busqueda ?? '', { emitEvent: false });
          }
          this.cargando.set(true);
          this.error.set(null);
        }),
        switchMap((consulta) =>
          this.cargar(consulta).pipe(
            catchError((err: HttpErrorResponse) => {
              this.error.set(leerError(err).mensaje);
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((pagina) => {
        this.cargando.set(false);
        if (!pagina) return;
        // La página quedó vacía (por ejemplo, se eliminó su último registro): se vuelve a la última que existe
        if (pagina.content.length === 0 && pagina.page > 0) {
          this.irA(Math.max(0, pagina.totalPages - 1));
          return;
        }
        this.pagina.set(pagina);
      });
  }

  irA(page: number): void {
    this.navegar({ page } as Partial<F>);
  }

  cambiarTamano(size: number): void {
    this.navegar({ size, page: 0 } as Partial<F>);
  }

  ordenar(sort: string): void {
    this.navegar({ sort, page: 0 } as Partial<F>);
  }

  filtrar(cambios: Partial<F>): void {
    this.navegar({ ...cambios, page: 0 });
  }

  limpiarFiltros(): void {
    this.busqueda.setValue('', { emitEvent: false });
    this.navegar({ ...this.defectos, size: this.consulta().size, sort: this.consulta().sort });
  }

  /** Vuelve a pedir la página actual (después de crear, editar o eliminar). */
  recargar(): void {
    this.recargar$.next();
  }

  /** Recarga tras eliminar: si era el único registro de la página, vuelve a la anterior. */
  recargarTrasEliminar(): void {
    const p = this.pagina();
    if (p && p.content.length === 1 && p.page > 0) this.irA(p.page - 1);
    else this.recargar();
  }

  /** Actualiza la URL; los valores por defecto se omiten para que la URL quede limpia. */
  private navegar(cambios: Partial<F>): void {
    const c = { ...this.consulta(), ...cambios } as F;
    const d = this.defectos;
    const queryParams: Params = {
      pagina: c.page > 0 ? c.page + 1 : null,
      tamano: c.size !== d.size ? c.size : null,
      orden: c.sort !== d.sort ? c.sort : null,
      q: c.busqueda ? c.busqueda : null,
    };
    for (const f of this.filtros) {
      const valor = c[f];
      queryParams[f] = valor !== undefined && valor !== null && valor !== '' && valor !== d[f] ? valor : null;
    }
    this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge', replaceUrl: false });
  }

  /** Lee la consulta desde la URL, validando los valores (página ≥ 1, tamaño permitido). */
  private leer(p: ParamMap): F {
    const pagina = Number(p.get('pagina'));
    const tamano = Number(p.get('tamano'));
    const consulta = {
      ...this.defectos,
      page: Number.isInteger(pagina) && pagina >= 1 ? pagina - 1 : 0,
      size: TAMANOS_PAGINA.includes(tamano) ? tamano : this.defectos.size,
      sort: p.get('orden') || this.defectos.sort,
      busqueda: p.get('q') ?? '',
    } as F;
    for (const f of this.filtros) {
      (consulta as Record<string, unknown>)[f] = p.get(f) ?? this.defectos[f];
    }
    return consulta;
  }
}
