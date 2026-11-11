import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { EquipoService } from '../../core/services/equipo.service';
import { SolicitanteService } from '../../core/services/solicitante.service';
import { PrestamoService } from '../../core/services/prestamo.service';
import { Auth } from '../../core/services/auth';
import { Equipo, Prestamo, Solicitante } from '../../core/models';
import { Disponibilidad } from '../../shared/disponibilidad';
import { EtiquetaPipe } from '../../shared/etiqueta-pipe';

@Component({
  selector: 'app-inicio',
  imports: [RouterLink, DatePipe, NgClass, Disponibilidad, EtiquetaPipe],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio implements OnInit {
  private equipoService = inject(EquipoService);
  private solicitanteService = inject(SolicitanteService);
  private prestamoService = inject(PrestamoService);
  protected readonly auth = inject(Auth);

  protected readonly equipos = signal<Equipo[]>([]);
  protected readonly solicitantes = signal<Solicitante[]>([]);
  protected readonly prestamos = signal<Prestamo[]>([]);
  protected readonly cargando = signal(true);

  protected readonly unidadesTotales = computed(() =>
    this.equipos().reduce((suma, e) => suma + e.cantidadTotal, 0),
  );
  protected readonly unidadesDisponibles = computed(() =>
    this.equipos().reduce((suma, e) => suma + (e.estado === 'OPERATIVO' ? e.cantidadDisponible : 0), 0),
  );
  protected readonly activos = computed(() => this.prestamos().filter((p) => p.estado === 'ACTIVO').length);
  protected readonly vencidos = computed(() => this.prestamos().filter((p) => p.estado === 'VENCIDO').length);

  /** Préstamos sin devolver, primero los que vencen antes. */
  protected readonly porDevolver = computed(() =>
    this.prestamos()
      .filter((p) => p.estado !== 'DEVUELTO')
      .sort((a, b) => a.fechaDevolucionPrevista.localeCompare(b.fechaDevolucionPrevista))
      .slice(0, 6),
  );
  protected readonly pocaDisponibilidad = computed(() =>
    this.equipos().filter((e) => e.estado === 'OPERATIVO' && e.cantidadDisponible <= 2),
  );

  protected readonly fecha = new Date();

  ngOnInit(): void {
    forkJoin({
      equipos: this.equipoService.listar(),
      solicitantes: this.solicitanteService.listar(),
      prestamos: this.prestamoService.listar(),
    }).subscribe({
      next: (r) => {
        this.equipos.set(r.equipos);
        this.solicitantes.set(r.solicitantes);
        this.prestamos.set(r.prestamos);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
