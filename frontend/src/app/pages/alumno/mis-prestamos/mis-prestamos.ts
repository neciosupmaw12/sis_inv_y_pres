import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { PrestamoService } from '../../../core/services/prestamo.service';
import { leerError } from '../../../core/services/http-error';
import {
  CLASE_ESTADO_PRESTAMO,
  ConteoPrestamos,
  contarEstado,
  EstadoPrestamo,
  FiltrosPrestamo,
  Prestamo,
} from '../../../core/models';
import { EtiquetaPipe } from '../../../shared/etiqueta-pipe';
import { ListadoPaginado } from '../../../shared/listado-paginado';
import { Paginacion } from '../../../shared/paginacion/paginacion';
import { CabeceraOrden } from '../../../shared/cabecera-orden';

type Filtro = 'TODOS' | EstadoPrestamo;

/**
 * Préstamos y solicitudes del alumno (GET /api/prestamos/mis-prestamos), paginados y filtrados por estado en la URL.
 * El backend toma el solicitante del JWT: aquí no se envía ningún id.
 */
@Component({
  selector: 'app-mis-prestamos',
  imports: [DatePipe, NgClass, RouterLink, EtiquetaPipe, Paginacion, CabeceraOrden],
  templateUrl: './mis-prestamos.html',
  styleUrl: './mis-prestamos.scss',
})
export class MisPrestamos implements OnInit {
  private prestamoService = inject(PrestamoService);

  protected readonly filtros: Filtro[] = ['TODOS', 'PENDIENTE', 'ACTIVO', 'VENCIDO', 'DEVUELTO', 'RECHAZADO', 'CANCELADO'];
  protected readonly claseEstado = CLASE_ESTADO_PRESTAMO;

  protected readonly listado = new ListadoPaginado<FiltrosPrestamo, Prestamo>(
    { page: 0, size: 10, sort: 'fechaPrestamo,desc', busqueda: '', estado: '' },
    ['estado'],
    (consulta) => this.prestamoService.misPrestamos(consulta),
  );
  protected readonly conteo = signal<ConteoPrestamos | null>(null);
  protected readonly filtroActual = computed<Filtro>(() => this.listado.consulta().estado || 'TODOS');
  protected readonly abierto = signal<number | null>(null);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger'; texto: string } | null>(null);

  ngOnInit(): void {
    this.cargarConteo();
    // Viene de "Solicitar préstamo" (router state): confirma el envío
    const creada = (history.state as { creada?: number } | null)?.creada;
    if (creada) {
      this.notificar('success', `Solicitud #${creada} enviada. Queda PENDIENTE hasta que el administrador la apruebe.`);
    }
  }

  cargarConteo(): void {
    this.prestamoService.misResumen().subscribe((c) => this.conteo.set(c));
  }

  filtrar(f: Filtro): void {
    this.listado.filtrar({ estado: f === 'TODOS' ? '' : f });
  }

  contar(f: Filtro): number | null {
    const c = this.conteo();
    return c ? contarEstado(c, f) : null;
  }

  alternar(p: Prestamo): void {
    this.abierto.set(this.abierto() === p.id ? null : p.id);
  }

  resumenEquipos(p: Prestamo): string {
    return p.detalles.map((d) => `${d.cantidad}× ${d.equipo}`).join(', ');
  }

  cancelar(p: Prestamo): void {
    if (!confirm(`¿Cancelar tu solicitud #${p.id}? No se puede deshacer.`)) return;
    this.prestamoService.cancelar(p.id).subscribe({
      next: () => {
        this.notificar('success', `Solicitud #${p.id} cancelada`);
        this.listado.recargar();
        this.cargarConteo();
      },
      error: (err: HttpErrorResponse) => this.notificar('danger', leerError(err).mensaje),
    });
  }

  private notificar(tipo: 'success' | 'danger', texto: string): void {
    this.mensaje.set({ tipo, texto });
    setTimeout(() => this.mensaje.set(null), 6000);
  }
}
