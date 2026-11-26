import { inject, Injectable, signal } from '@angular/core';
import { PrestamoService } from './prestamo.service';

/**
 * Contador de solicitudes PENDIENTES que muestra el navbar del ADMIN. Lo actualiza el navbar al navegar (y cada
 * minuto) y la pantalla de solicitudes después de aprobar o rechazar.
 */
@Injectable({ providedIn: 'root' })
export class SolicitudesPendientes {
  private prestamoService = inject(PrestamoService);

  readonly cantidad = signal(0);

  actualizar(): void {
    this.prestamoService.resumen().subscribe({
      next: (c) => this.cantidad.set(c.pendientes),
      error: () => {},
    });
  }
}
