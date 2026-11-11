import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { EquipoService } from '../../core/services/equipo.service';
import { leerError } from '../../core/services/http-error';
import { Auth } from '../../core/services/auth';
import { Equipo, EstadoEquipo } from '../../core/models';
import { Disponibilidad } from '../../shared/disponibilidad';
import { EtiquetaPipe } from '../../shared/etiqueta-pipe';

@Component({
  selector: 'app-equipos',
  imports: [ReactiveFormsModule, FormsModule, NgClass, Disponibilidad, EtiquetaPipe],
  templateUrl: './equipos.html',
  styleUrl: './equipos.scss',
})
export class Equipos implements OnInit {
  private equipoService = inject(EquipoService);
  protected readonly auth = inject(Auth);

  protected readonly categorias = ['Laptops', 'Proyectores', 'Tablets', 'Periféricos', 'Audio', 'Redes', 'Otros'];
  protected readonly estados: EstadoEquipo[] = ['OPERATIVO', 'EN_MANTENIMIENTO', 'DE_BAJA'];

  protected readonly equipos = signal<Equipo[]>([]);
  protected readonly busqueda = signal('');
  protected readonly filtroCategoria = signal('');
  protected readonly editandoId = signal<number | null>(null);
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger'; texto: string } | null>(null);
  protected readonly erroresServidor = signal<Record<string, string>>({});

  protected readonly filtrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    const cat = this.filtroCategoria();
    return this.equipos().filter(
      (e) =>
        (!cat || e.categoria === cat) &&
        (!q ||
          e.nombre.toLowerCase().includes(q) ||
          e.codigo.toLowerCase().includes(q) ||
          (e.marca ?? '').toLowerCase().includes(q)),
    );
  });

  protected readonly form = inject(FormBuilder).group({
    codigo: ['', [Validators.required, Validators.maxLength(20)]],
    nombre: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    categoria: ['', [Validators.required, Validators.maxLength(50)]],
    marca: ['', [Validators.maxLength(50)]],
    modelo: ['', [Validators.maxLength(50)]],
    descripcion: ['', [Validators.maxLength(255)]],
    cantidadTotal: [null as number | null, [Validators.required, Validators.min(1), Validators.max(1000)]],
    estado: ['OPERATIVO' as EstadoEquipo, Validators.required],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.equipoService.listar().subscribe((data) => this.equipos.set(data));
  }

  editar(e: Equipo): void {
    this.editandoId.set(e.id!);
    this.erroresServidor.set({});
    this.form.reset({
      ...e,
      marca: e.marca ?? '',
      modelo: e.modelo ?? '',
      descripcion: e.descripcion ?? '',
    });
  }

  cancelar(): void {
    this.editandoId.set(null);
    this.erroresServidor.set({});
    this.form.reset({ estado: 'OPERATIVO' });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    const id = this.editandoId();
    const peticion = id
      ? this.equipoService.actualizar(id, this.form.value as Partial<Equipo>)
      : this.equipoService.crear(this.form.value as Partial<Equipo>);

    peticion.subscribe({
      next: (e) => {
        this.notificar('success', `Equipo "${e.nombre}" ${id ? 'actualizado' : 'registrado'} correctamente`);
        this.cancelar();
        this.cargar();
        this.guardando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const e = leerError(err);
        this.erroresServidor.set(e.errores ?? {});
        this.notificar('danger', e.mensaje);
        this.guardando.set(false);
      },
    });
  }

  eliminar(e: Equipo): void {
    if (!confirm(`¿Eliminar el equipo "${e.nombre}"?`)) return;
    this.equipoService.eliminar(e.id!).subscribe({
      next: () => {
        this.notificar('success', 'Equipo eliminado');
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.notificar('danger', leerError(err).mensaje),
    });
  }

  claseEstado(estado: EstadoEquipo): string {
    return { OPERATIVO: 'text-bg-success', EN_MANTENIMIENTO: 'text-bg-warning', DE_BAJA: 'text-bg-secondary' }[estado];
  }

  invalido(campo: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[campo];
    return (c.touched && c.invalid) || !!this.erroresServidor()[campo];
  }

  private notificar(tipo: 'success' | 'danger', texto: string): void {
    this.mensaje.set({ tipo, texto });
    setTimeout(() => this.mensaje.set(null), 5000);
  }
}
