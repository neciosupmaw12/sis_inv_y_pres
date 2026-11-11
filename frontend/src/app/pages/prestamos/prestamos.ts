import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe, NgClass } from '@angular/common';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { EquipoService } from '../../core/services/equipo.service';
import { SolicitanteService } from '../../core/services/solicitante.service';
import { PrestamoService } from '../../core/services/prestamo.service';
import { leerError } from '../../core/services/http-error';
import { Auth } from '../../core/services/auth';
import { Equipo, EstadoPrestamo, Prestamo, PrestamoRequest, Solicitante } from '../../core/models';
import { EtiquetaPipe } from '../../shared/etiqueta-pipe';

type Filtro = 'TODOS' | EstadoPrestamo;

/** Fecha local en formato yyyy-mm-dd (la que espera <input type="date">). */
function fechaLocal(dias = 0): string {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

@Component({
  selector: 'app-prestamos',
  imports: [ReactiveFormsModule, DatePipe, NgClass, EtiquetaPipe],
  templateUrl: './prestamos.html',
  styleUrl: './prestamos.scss',
})
export class Prestamos implements OnInit {
  private prestamoService = inject(PrestamoService);
  private solicitanteService = inject(SolicitanteService);
  private equipoService = inject(EquipoService);
  private fb = inject(FormBuilder);
  protected readonly auth = inject(Auth);

  protected readonly hoy = fechaLocal();
  protected readonly filtros: Filtro[] = ['TODOS', 'ACTIVO', 'VENCIDO', 'DEVUELTO'];

  protected readonly prestamos = signal<Prestamo[]>([]);
  protected readonly solicitantes = signal<Solicitante[]>([]);
  protected readonly equipos = signal<Equipo[]>([]);
  protected readonly filtro = signal<Filtro>('TODOS');
  protected readonly detalle = signal<Prestamo | null>(null);
  protected readonly editandoId = signal<number | null>(null);
  protected readonly mostrarFormulario = signal(false);
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger'; texto: string } | null>(null);

  protected readonly filtrados = computed(() =>
    this.filtro() === 'TODOS' ? this.prestamos() : this.prestamos().filter((p) => p.estado === this.filtro()),
  );
  protected readonly equiposPrestables = computed(() => this.equipos().filter((e) => e.estado === 'OPERATIVO'));

  protected readonly form = this.fb.group({
    solicitanteId: [null as number | null, Validators.required],
    fechaDevolucionPrevista: [fechaLocal(7), Validators.required],
    observaciones: ['', Validators.maxLength(255)],
    detalles: this.fb.array([this.nuevaLinea()]),
  });

  get lineas(): FormArray {
    return this.form.controls.detalles;
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    forkJoin({
      prestamos: this.prestamoService.listar(),
      solicitantes: this.solicitanteService.listar(),
      equipos: this.equipoService.listar(),
    }).subscribe((r) => {
      this.prestamos.set(r.prestamos);
      this.solicitantes.set(r.solicitantes);
      this.equipos.set(r.equipos);
    });
  }

  contar(f: Filtro): number {
    return f === 'TODOS' ? this.prestamos().length : this.prestamos().filter((p) => p.estado === f).length;
  }

  nuevaLinea(equipoId: number | null = null, cantidad = 1) {
    return this.fb.group({
      equipoId: [equipoId, Validators.required],
      cantidad: [cantidad, [Validators.required, Validators.min(1), Validators.max(100)]],
    });
  }

  agregarLinea(): void {
    this.lineas.push(this.nuevaLinea());
  }

  quitarLinea(i: number): void {
    if (this.lineas.length > 1) this.lineas.removeAt(i);
  }

  equipo(id: number | null): Equipo | undefined {
    return this.equipos().find((e) => e.id === Number(id));
  }

  totalUnidades(): number {
    return this.lineas.controls.reduce((suma, c) => suma + (Number(c.value.cantidad) || 0), 0);
  }

  abrirNuevo(): void {
    this.editandoId.set(null);
    this.detalle.set(null);
    this.form.reset({ fechaDevolucionPrevista: fechaLocal(7), observaciones: '' });
    this.lineas.clear();
    this.lineas.push(this.nuevaLinea());
    this.mostrarFormulario.set(true);
  }

  ver(p: Prestamo): void {
    this.mostrarFormulario.set(false);
    this.detalle.set(p);
  }

  editar(p: Prestamo): void {
    this.detalle.set(null);
    this.editandoId.set(p.id);
    this.form.patchValue({
      solicitanteId: p.solicitanteId,
      fechaDevolucionPrevista: p.fechaDevolucionPrevista,
      observaciones: p.observaciones ?? '',
    });
    this.lineas.clear();
    p.detalles.forEach((d) => this.lineas.push(this.nuevaLinea(d.equipoId, d.cantidad)));
    this.mostrarFormulario.set(true);
  }

  cerrar(): void {
    this.mostrarFormulario.set(false);
    this.detalle.set(null);
    this.editandoId.set(null);
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body: PrestamoRequest = {
      solicitanteId: Number(v.solicitanteId),
      fechaDevolucionPrevista: v.fechaDevolucionPrevista!,
      observaciones: v.observaciones || null,
      detalles: this.lineas.value.map((l: { equipoId: number; cantidad: number }) => ({
        equipoId: Number(l.equipoId),
        cantidad: Number(l.cantidad),
      })),
    };
    const id = this.editandoId();
    this.guardando.set(true);
    const peticion = id
      ? this.prestamoService.actualizar(id, body)
      : this.prestamoService.crear(body);
    peticion.subscribe({
      next: (p) => {
        this.notificar('success', `Préstamo #${p.id} ${id ? 'actualizado' : 'registrado'}: ${p.totalEquipos} equipo(s) para ${p.solicitante}`);
        this.guardando.set(false);
        this.cerrar();
        this.cargar();
      },
      error: (err: HttpErrorResponse) => {
        const e = leerError(err);
        const detalleCampos = e.errores ? ' ' + Object.values(e.errores).join('. ') : '';
        this.notificar('danger', e.mensaje + detalleCampos);
        this.guardando.set(false);
      },
    });
  }

  devolver(p: Prestamo): void {
    if (!confirm(`¿Registrar la devolución del préstamo #${p.id} de ${p.solicitante}?`)) return;
    this.prestamoService.devolver(p.id).subscribe({
      next: () => {
        this.notificar('success', `Devolución del préstamo #${p.id} registrada; los equipos vuelven al inventario`);
        this.cerrar();
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.notificar('danger', leerError(err).mensaje),
    });
  }

  anular(p: Prestamo): void {
    if (!confirm(`¿Anular el préstamo #${p.id}? Se eliminará del historial.`)) return;
    this.prestamoService.eliminar(p.id).subscribe({
      next: () => {
        this.notificar('success', `Préstamo #${p.id} anulado`);
        this.cerrar();
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.notificar('danger', leerError(err).mensaje),
    });
  }

  claseEstado(estado: EstadoPrestamo): string {
    return { ACTIVO: 'text-bg-primary', VENCIDO: 'text-bg-danger', DEVUELTO: 'text-bg-success' }[estado];
  }

  private notificar(tipo: 'success' | 'danger', texto: string): void {
    this.mensaje.set({ tipo, texto });
    setTimeout(() => this.mensaje.set(null), 6000);
  }
}
