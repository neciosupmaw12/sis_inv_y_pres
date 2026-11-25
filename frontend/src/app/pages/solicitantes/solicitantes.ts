import { Component, computed, inject, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { SolicitanteService } from '../../core/services/solicitante.service';
import { leerError } from '../../core/services/http-error';
import { Auth } from '../../core/services/auth';
import { FiltrosSolicitante, Solicitante, TipoSolicitante } from '../../core/models';
import { EtiquetaPipe } from '../../shared/etiqueta-pipe';
import { Paginacion } from '../../shared/paginacion/paginacion';
import { ListadoPaginado } from '../../shared/listado-paginado';

@Component({
  selector: 'app-solicitantes',
  imports: [ReactiveFormsModule, NgClass, EtiquetaPipe, Paginacion],
  templateUrl: './solicitantes.html',
  styleUrl: './solicitantes.scss',
})
export class Solicitantes {
  private solicitanteService = inject(SolicitanteService);
  protected readonly auth = inject(Auth);

  protected readonly tipos: TipoSolicitante[] = ['ESTUDIANTE', 'DOCENTE', 'ADMINISTRATIVO'];

  /** Campos por los que se puede ordenar (coinciden con la lista blanca del backend). */
  protected readonly ordenes = [
    { campo: 'apellidos', texto: 'Apellidos' },
    { campo: 'nombres', texto: 'Nombres' },
    { campo: 'dni', texto: 'DNI' },
    { campo: 'tipo', texto: 'Tipo' },
  ];

  /** Listado paginado desde Spring Boot (GET /api/solicitantes/paginado); su estado vive en la URL. */
  protected readonly listado = new ListadoPaginado<FiltrosSolicitante, Solicitante>(
    { page: 0, size: 10, sort: 'apellidos,asc', busqueda: '', tipo: '' },
    ['tipo'],
    (consulta) => this.solicitanteService.listarPaginado(consulta),
  );
  protected readonly campoOrden = computed(() => this.listado.consulta().sort.split(',')[0]);
  protected readonly ordenDescendente = computed(() => this.listado.consulta().sort.endsWith(',desc'));

  protected readonly editandoId = signal<number | null>(null);
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger'; texto: string } | null>(null);
  protected readonly erroresServidor = signal<Record<string, string>>({});

  protected readonly form = inject(FormBuilder).group({
    dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    nombres: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    apellidos: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    tipo: ['ESTUDIANTE' as TipoSolicitante, Validators.required],
    area: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.email, Validators.maxLength(100)]],
    telefono: ['', [Validators.pattern(/^9\d{8}$/)]],
  });

  cambiarCampoOrden(campo: string): void {
    this.listado.ordenar(`${campo},${this.ordenDescendente() ? 'desc' : 'asc'}`);
  }

  alternarDireccion(): void {
    this.listado.ordenar(`${this.campoOrden()},${this.ordenDescendente() ? 'asc' : 'desc'}`);
  }

  editar(s: Solicitante): void {
    this.editandoId.set(s.id!);
    this.erroresServidor.set({});
    this.form.reset({ ...s, area: s.area ?? '', email: s.email ?? '', telefono: s.telefono ?? '' });
  }

  cancelar(): void {
    this.editandoId.set(null);
    this.erroresServidor.set({});
    this.form.reset({ tipo: 'ESTUDIANTE' });
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    const id = this.editandoId();
    const peticion = id
      ? this.solicitanteService.actualizar(id, this.form.value as Partial<Solicitante>)
      : this.solicitanteService.crear(this.form.value as Partial<Solicitante>);

    peticion.subscribe({
      next: (s) => {
        this.notificar('success', `Solicitante ${s.nombres} ${s.apellidos} ${id ? 'actualizado' : 'registrado'}`);
        this.cancelar();
        this.listado.recargar();
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

  eliminar(s: Solicitante): void {
    if (!confirm(`¿Eliminar al solicitante ${s.nombres} ${s.apellidos}?`)) return;
    this.solicitanteService.eliminar(s.id!).subscribe({
      next: () => {
        this.notificar('success', 'Solicitante eliminado');
        if (this.editandoId() === s.id) this.cancelar();
        this.listado.recargarTrasEliminar();
      },
      error: (err: HttpErrorResponse) => this.notificar('danger', leerError(err).mensaje),
    });
  }

  invalido(campo: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[campo];
    return (c.touched && c.invalid) || !!this.erroresServidor()[campo];
  }

  iniciales(s: Solicitante): string {
    return (s.nombres[0] + s.apellidos[0]).toUpperCase();
  }

  private notificar(tipo: 'success' | 'danger', texto: string): void {
    this.mensaje.set({ tipo, texto });
    setTimeout(() => this.mensaje.set(null), 5000);
  }
}
