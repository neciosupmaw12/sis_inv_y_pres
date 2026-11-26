import { Component, HostListener, inject, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { PrestamoService } from '../../core/services/prestamo.service';
import { SolicitudesPendientes } from '../../core/services/solicitudes-pendientes';
import { leerError } from '../../core/services/http-error';
import { FiltrosPrestamo, Prestamo } from '../../core/models';
import { EtiquetaPipe } from '../../shared/etiqueta-pipe';
import { ListadoPaginado } from '../../shared/listado-paginado';
import { Paginacion } from '../../shared/paginacion/paginacion';
import { fechaLocal } from '../../shared/fechas';

/**
 * Solicitudes PENDIENTES de los alumnos (solo ADMIN): las más antiguas primero, con el stock actual de cada equipo y
 * los botones Aprobar (PUT /aprobar, transacción con rollback si no alcanza el stock) y Rechazar (modal con motivo).
 */
@Component({
  selector: 'app-solicitudes',
  imports: [DatePipe, NgClass, ReactiveFormsModule, EtiquetaPipe, Paginacion],
  templateUrl: './solicitudes.html',
  styleUrl: './solicitudes.scss',
})
export class Solicitudes {
  private prestamoService = inject(PrestamoService);
  private pendientes = inject(SolicitudesPendientes);

  protected readonly hoy = fechaLocal();

  protected readonly listado = new ListadoPaginado<FiltrosPrestamo, Prestamo>(
    { page: 0, size: 10, sort: 'fechaPrestamo,asc', busqueda: '', estado: 'PENDIENTE' },
    [],
    (consulta) => this.prestamoService.listarPaginado({ ...consulta, estado: 'PENDIENTE' }),
  );

  /** Id de la solicitud que se está aprobando o rechazando (deshabilita sus botones). */
  protected readonly procesando = signal<number | null>(null);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger' | 'warning'; texto: string } | null>(null);

  // Modal de rechazo
  protected readonly rechazando = signal<Prestamo | null>(null);
  protected readonly errorRechazo = signal<string | null>(null);
  protected readonly formRechazo = new FormGroup({
    motivo: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(255)],
    }),
  });

  protected get motivo(): FormControl<string> {
    return this.formRechazo.controls.motivo;
  }

  /** ¿Hay stock y todos los equipos están operativos? Si no, la aprobación responderá 409/400. */
  alcanza(p: Prestamo): boolean {
    return p.detalles.every((d) => d.estadoEquipo === 'OPERATIVO' && d.disponibles >= d.cantidad);
  }

  fechaPasada(p: Prestamo): boolean {
    return p.fechaDevolucionPrevista < this.hoy;
  }

  aprobar(p: Prestamo): void {
    if (!confirm(`¿Aprobar la solicitud #${p.id} de ${p.solicitante}? Se descontarán ${p.totalEquipos} unidad(es) del stock.`)) {
      return;
    }
    this.procesando.set(p.id);
    this.prestamoService.aprobar(p.id).subscribe({
      next: (r) => {
        this.notificar('success', `Solicitud #${r.id} aprobada: el préstamo está ACTIVO y se descontaron ${r.totalEquipos} unidad(es).`);
        this.terminar();
      },
      error: (err: HttpErrorResponse) => {
        const e = leerError(err);
        if (e.status === 409) {
          // Stock insuficiente o modificación simultánea: la transacción hizo ROLLBACK
          this.notificar('danger', `Solicitud #${p.id}: ${e.mensaje} Sigue pendiente y no se descontó ningún equipo: puede rechazarla o esperar a que se devuelvan unidades.`);
        } else {
          this.notificar('danger', `No se pudo aprobar la solicitud #${p.id}: ${e.mensaje}`);
        }
        this.terminar();
      },
    });
  }

  abrirRechazo(p: Prestamo): void {
    this.motivo.reset('');
    this.errorRechazo.set(null);
    this.rechazando.set(p);
  }

  @HostListener('document:keydown.escape')
  cerrarRechazo(): void {
    if (this.procesando() === null) this.rechazando.set(null);
  }

  confirmarRechazo(): void {
    const p = this.rechazando();
    if (!p) return;
    if (this.motivo.invalid) {
      this.motivo.markAsTouched();
      return;
    }
    this.procesando.set(p.id);
    this.prestamoService.rechazar(p.id, this.motivo.value.trim()).subscribe({
      next: () => {
        this.rechazando.set(null);
        this.notificar('warning', `Solicitud #${p.id} rechazada. El alumno verá el motivo en "Mis préstamos".`);
        this.terminar();
      },
      error: (err: HttpErrorResponse) => {
        const e = leerError(err);
        this.errorRechazo.set(e.errores?.['motivo'] ?? e.mensaje);
        this.procesando.set(null);
      },
    });
  }

  /** Recarga la lista (el stock pudo cambiar) y el contador del navbar. */
  private terminar(): void {
    this.procesando.set(null);
    this.listado.recargarTrasEliminar();
    this.pendientes.actualizar();
  }

  private notificar(tipo: 'success' | 'danger' | 'warning', texto: string): void {
    this.mensaje.set({ tipo, texto });
    setTimeout(() => this.mensaje.set(null), 9000);
  }
}
