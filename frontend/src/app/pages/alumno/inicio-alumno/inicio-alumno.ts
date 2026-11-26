import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Auth } from '../../../core/services/auth';
import { PrestamoService } from '../../../core/services/prestamo.service';
import { CLASE_ESTADO_PRESTAMO, ConteoPrestamos, LIMITES_SOLICITUD, Prestamo } from '../../../core/models';
import { EtiquetaPipe } from '../../../shared/etiqueta-pipe';

/** Inicio del alumno: resumen de SUS préstamos (GET /api/prestamos/mis-prestamos/resumen) y los últimos movimientos. */
@Component({
  selector: 'app-inicio-alumno',
  imports: [RouterLink, DatePipe, NgClass, EtiquetaPipe],
  templateUrl: './inicio-alumno.html',
  styleUrl: './inicio-alumno.scss',
})
export class InicioAlumno implements OnInit {
  private prestamoService = inject(PrestamoService);
  protected readonly auth = inject(Auth);

  protected readonly fecha = new Date();
  protected readonly limites = LIMITES_SOLICITUD;
  protected readonly claseEstado = CLASE_ESTADO_PRESTAMO;
  protected readonly cargando = signal(true);
  protected readonly conteo = signal<ConteoPrestamos | null>(null);
  protected readonly recientes = signal<Prestamo[]>([]);

  ngOnInit(): void {
    forkJoin({
      conteo: this.prestamoService.misResumen(),
      recientes: this.prestamoService.misPrestamos({ page: 0, size: 5, sort: 'fechaPrestamo,desc' }),
    }).subscribe({
      next: (r) => {
        this.conteo.set(r.conteo);
        this.recientes.set(r.recientes.content);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
