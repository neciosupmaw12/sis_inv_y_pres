import { Component, inject, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { PerfilService } from '../../../core/services/perfil.service';
import { leerError } from '../../../core/services/http-error';
import { Perfil } from '../../../core/models';

/** Perfil del alumno: ve sus datos y edita solo su correo y su teléfono (GET y PUT /api/perfil). */
@Component({
  selector: 'app-mi-perfil',
  imports: [ReactiveFormsModule, NgClass],
  templateUrl: './mi-perfil.html',
})
export class MiPerfil implements OnInit {
  private perfilService = inject(PerfilService);

  protected readonly perfil = signal<Perfil | null>(null);
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger'; texto: string } | null>(null);
  protected readonly erroresServidor = signal<Record<string, string>>({});

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    telefono: ['', Validators.pattern(/^$|^9\d{8}$/)],
  });

  ngOnInit(): void {
    this.perfilService.obtener().subscribe((p) => this.mostrar(p));
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.perfilService.actualizar(this.form.getRawValue()).subscribe({
      next: (p) => {
        this.mostrar(p);
        this.erroresServidor.set({});
        this.notificar('success', 'Datos de contacto actualizados');
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

  invalido(campo: 'email' | 'telefono'): boolean {
    const c = this.form.controls[campo];
    return (c.touched && c.invalid) || !!this.erroresServidor()[campo];
  }

  private mostrar(p: Perfil): void {
    this.perfil.set(p);
    this.form.reset({ email: p.email ?? '', telefono: p.telefono ?? '' });
  }

  private notificar(tipo: 'success' | 'danger', texto: string): void {
    this.mensaje.set({ tipo, texto });
    setTimeout(() => this.mensaje.set(null), 4000);
  }
}
