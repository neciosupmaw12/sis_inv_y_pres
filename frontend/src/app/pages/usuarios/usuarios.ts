import { Component, inject, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RegistroUsuario, UsuarioService } from '../../core/services/usuario.service';
import { leerError } from '../../core/services/http-error';
import { Auth } from '../../core/services/auth';
import { UsuarioSistema } from '../../core/models';

@Component({
  selector: 'app-usuarios',
  imports: [ReactiveFormsModule, NgClass],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.scss',
})
export class Usuarios implements OnInit {
  private usuarioService = inject(UsuarioService);
  protected readonly auth = inject(Auth);

  protected readonly usuarios = signal<UsuarioSistema[]>([]);
  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger'; texto: string } | null>(null);
  protected readonly erroresServidor = signal<Record<string, string>>({});

  protected readonly form = inject(FormBuilder).nonNullable.group({
    username: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._]{4,50}$/)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(60)]],
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    rol: ['ENCARGADO', Validators.required],
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.usuarioService.listar().subscribe((data) => this.usuarios.set(data));
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.guardando.set(true);
    this.usuarioService.crear(this.form.getRawValue() as RegistroUsuario).subscribe({
      next: (u) => {
        this.notificar('success', `Usuario "${u.username}" registrado`);
        this.erroresServidor.set({});
        this.form.reset();
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

  desactivar(u: UsuarioSistema): void {
    if (!confirm(`¿Desactivar al usuario "${u.username}"? Ya no podrá iniciar sesión.`)) return;
    this.usuarioService.desactivar(u.id).subscribe({
      next: () => {
        this.notificar('success', `Usuario "${u.username}" desactivado`);
        this.cargar();
      },
      error: (err: HttpErrorResponse) => this.notificar('danger', leerError(err).mensaje),
    });
  }

  invalido(campo: keyof typeof this.form.controls): boolean {
    const c = this.form.controls[campo];
    return (c.touched && c.invalid) || !!this.erroresServidor()[campo];
  }

  private notificar(tipo: 'success' | 'danger', texto: string): void {
    this.mensaje.set({ tipo, texto });
    setTimeout(() => this.mensaje.set(null), 4000);
  }
}
