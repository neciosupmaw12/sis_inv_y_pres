import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Api, leerError } from '../../core/services/api';
import { Auth } from '../../core/services/auth';
import { Solicitante, TipoSolicitante } from '../../core/models';
import { EtiquetaPipe } from '../../shared/etiqueta-pipe';

@Component({
  selector: 'app-solicitantes',
  imports: [ReactiveFormsModule, FormsModule, NgClass, EtiquetaPipe],
  templateUrl: './solicitantes.html',
  styleUrl: './solicitantes.scss',
})
export class Solicitantes implements OnInit {
  private api = inject(Api);
  protected readonly auth = inject(Auth);

  protected readonly tipos: TipoSolicitante[] = ['ESTUDIANTE', 'DOCENTE', 'ADMINISTRATIVO'];

  protected readonly solicitantes = signal<Solicitante[]>([]);
  protected readonly busqueda = signal('');
  protected readonly editandoId = signal<number | null>(null);
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger'; texto: string } | null>(null);
  protected readonly erroresServidor = signal<Record<string, string>>({});

  protected readonly filtrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    return this.solicitantes().filter(
      (s) => !q || s.dni.includes(q) || `${s.nombres} ${s.apellidos}`.toLowerCase().includes(q),
    );
  });

  protected readonly form = inject(FormBuilder).group({
    dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
    nombres: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    apellidos: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    tipo: ['ESTUDIANTE' as TipoSolicitante, Validators.required],
    area: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.email, Validators.maxLength(100)]],
    telefono: ['', [Validators.pattern(/^9\d{8}$/)]],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.index<Solicitante>('solicitantes').subscribe((data) => this.solicitantes.set(data));
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
      ? this.api.update<Solicitante>('solicitantes', id, this.form.value)
      : this.api.create<Solicitante>('solicitantes', this.form.value);

    peticion.subscribe({
      next: (s) => {
        this.notificar('success', `Solicitante ${s.nombres} ${s.apellidos} ${id ? 'actualizado' : 'registrado'}`);
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

  eliminar(s: Solicitante): void {
    if (!confirm(`¿Eliminar al solicitante ${s.nombres} ${s.apellidos}?`)) return;
    this.api.delete('solicitantes', s.id!).subscribe({
      next: () => {
        this.notificar('success', 'Solicitante eliminado');
        this.cargar();
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
