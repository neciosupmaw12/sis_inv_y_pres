import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { EquipoService } from '../../core/services/equipo.service';
import { SolicitanteService } from '../../core/services/solicitante.service';
import { PrestamoService } from '../../core/services/prestamo.service';
import { Auth } from '../../core/services/auth';
import { PanelService } from '../../core/services/panel.service';
import { CLASE_ESTADO_PRESTAMO, Equipo, PanelResumen, Prestamo, Solicitante } from '../../core/models';
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
  private panelService = inject(PanelService);
  protected readonly auth = inject(Auth);

  protected readonly equipos = signal<Equipo[]>([]);
  protected readonly solicitantes = signal<Solicitante[]>([]);
  protected readonly prestamos = signal<Prestamo[]>([]);
  protected readonly cargando = signal(true);
  /** Indicadores calculados en MySQL con JPQL (GROUP BY, SUM, CASE WHEN) en GET /api/panel/resumen. */
  protected readonly panel = signal<PanelResumen | null>(null);

  protected readonly unidadesTotales = computed(() => this.panel()?.unidadesTotales ?? 0);
  protected readonly unidadesDisponibles = computed(() => this.panel()?.unidadesDisponibles ?? 0);
  protected readonly activos = computed(() => this.panel()?.prestamos.activos ?? 0);
  protected readonly vencidos = computed(() => this.panel()?.prestamos.vencidos ?? 0);
  protected readonly pendientes = computed(() => this.panel()?.prestamos.pendientes ?? 0);
  protected readonly rechazados = computed(() => this.panel()?.prestamos.rechazados ?? 0);
  protected readonly claseEstado = CLASE_ESTADO_PRESTAMO;
  /** Para la barra del top: el equipo más prestado ocupa el 100 %. */
  protected readonly maximoPrestado = computed(() =>
    Math.max(1, ...(this.panel()?.topEquipos ?? []).map((e) => e.unidadesPrestadas)),
  );

  /** Préstamos entregados sin devolver (no solicitudes), primero los que vencen antes. */
  protected readonly porDevolver = computed(() =>
    this.prestamos()
      .filter((p) => p.estado === 'ACTIVO' || p.estado === 'VENCIDO')
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
      panel: this.panelService.resumen(),
    }).subscribe({
      next: (r) => {
        this.panel.set(r.panel);
        this.equipos.set(r.equipos);
        this.solicitantes.set(r.solicitantes);
        this.prestamos.set(r.prestamos);
        this.cargando.set(false);
      },
      error: () => this.cargando.set(false),
    });
  }
}
