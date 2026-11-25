import { Component, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { EquipoService } from '../../core/services/equipo.service';
import { leerError } from '../../core/services/http-error';
import { Auth } from '../../core/services/auth';
import { Equipo, EstadoEquipo, FiltrosEquipo } from '../../core/models';
import { EtiquetaPipe } from '../../shared/etiqueta-pipe';
import { Paginacion } from '../../shared/paginacion/paginacion';
import { CabeceraOrden } from '../../shared/cabecera-orden';
import { ListadoPaginado } from '../../shared/listado-paginado';

@Component({
  selector: 'app-equipos',
  imports: [ReactiveFormsModule, NgClass, EtiquetaPipe, Paginacion, CabeceraOrden],
  templateUrl: './equipos.html',
  styleUrl: './equipos.scss',
})
export class Equipos {
  private equipoService = inject(EquipoService);
  protected readonly auth = inject(Auth);

  protected readonly categorias = ['Laptops', 'Proyectores', 'Tablets', 'Periféricos', 'Audio', 'Redes', 'Otros'];
  protected readonly estados: EstadoEquipo[] = ['OPERATIVO', 'EN_MANTENIMIENTO', 'DE_BAJA'];

  /** Listado paginado desde Spring Boot (GET /api/equipos/paginado); su estado vive en la URL. */
  protected readonly listado = new ListadoPaginado<FiltrosEquipo, Equipo>(
    { page: 0, size: 10, sort: 'nombre,asc', busqueda: '', estado: '', categoria: '' },
    ['estado', 'categoria'],
    (consulta) => this.equipoService.listarPaginado(consulta),
  );

  protected readonly editandoId = signal<number | null>(null);
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger'; texto: string } | null>(null);
  protected readonly erroresServidor = signal<Record<string, string>>({});

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

  hayFiltros(): boolean {
    const c = this.listado.consulta();
    return !!(c.busqueda || c.estado || c.categoria);
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
        this.listado.recargar(); // se queda en la página actual
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
        if (this.editandoId() === e.id) this.cancelar();
        this.listado.recargarTrasEliminar(); // si la página queda vacía, vuelve a la anterior
      },
      error: (err: HttpErrorResponse) => this.notificar('danger', leerError(err).mensaje),
    });
  }

  claseEstado(estado: EstadoEquipo): string {
    return { OPERATIVO: 'text-bg-success', EN_MANTENIMIENTO: 'text-bg-warning', DE_BAJA: 'text-bg-secondary' }[estado];
  }

  /** Badge de stock: rojo si no quedan unidades, amarillo si quedan 2 o menos. */
  claseStock(e: Equipo): string {
    if (e.cantidadDisponible === 0) return 'text-bg-danger';
    if (e.cantidadDisponible <= 2) return 'text-bg-warning';
    return 'text-bg-light border';
  }

  textoStock(e: Equipo): string | null {
    if (e.cantidadDisponible === 0) return 'Agotado';
    if (e.cantidadDisponible <= 2) return 'Stock bajo';
    return null;
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
