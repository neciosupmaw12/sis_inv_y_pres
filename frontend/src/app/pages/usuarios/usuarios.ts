import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError, debounceTime, distinctUntilChanged, map, of, switchMap } from 'rxjs';
import { RegistroUsuario, UsuarioService } from '../../core/services/usuario.service';
import { SolicitanteService } from '../../core/services/solicitante.service';
import { leerError } from '../../core/services/http-error';
import { Auth } from '../../core/services/auth';
import { CLASE_ROL, Rol, Solicitante, UsuarioSistema } from '../../core/models';
import { EtiquetaPipe } from '../../shared/etiqueta-pipe';

type FiltroRol = 'TODOS' | Rol;
type CampoAlumno = 'dni' | 'nombres' | 'apellidos' | 'email' | 'telefono' | 'area';

@Component({
  selector: 'app-usuarios',
  imports: [ReactiveFormsModule, NgClass, EtiquetaPipe],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.scss',
})
export class Usuarios implements OnInit {
  private usuarioService = inject(UsuarioService);
  private solicitanteService = inject(SolicitanteService);
  private destroyRef = inject(DestroyRef);
  private fb = inject(FormBuilder);
  protected readonly auth = inject(Auth);

  protected readonly filtrosRol: FiltroRol[] = ['TODOS', 'ADMIN', 'ENCARGADO', 'ALUMNO'];
  protected readonly claseRol = CLASE_ROL;

  protected readonly usuarios = signal<UsuarioSistema[]>([]);
  protected readonly filtroRol = signal<FiltroRol>('TODOS');
  protected readonly visibles = computed(() => {
    const filtro = this.filtroRol();
    return filtro === 'TODOS' ? this.usuarios() : this.usuarios().filter((u) => u.rol === filtro);
  });

  protected readonly guardando = signal(false);
  protected readonly mensaje = signal<{ tipo: 'success' | 'danger'; texto: string } | null>(null);
  protected readonly erroresServidor = signal<Record<string, string>>({});

  /** Estudiante que ya existe con el DNI escrito: se vinculará en lugar de crear otro. */
  protected readonly existente = signal<Solicitante | null>(null);
  /** Aviso si el DNI no se puede usar (es de un docente o el estudiante ya tiene usuario). */
  protected readonly avisoDni = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    rol: ['ENCARGADO' as Rol, Validators.required],
    username: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9._]{4,50}$/)]],
    password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(60)]],
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    // Datos del estudiante: solo se habilitan (y validan) con el rol ALUMNO
    alumno: this.fb.nonNullable.group({
      dni: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
      nombres: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
      apellidos: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      telefono: ['', Validators.pattern(/^$|^9\d{8}$/)],
      area: ['', [Validators.required, Validators.maxLength(100)]],
    }),
  });

  protected readonly esAlumno = signal(false);

  constructor() {
    this.form.controls.rol.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((rol) => this.aplicarRol(rol));
    this.aplicarRol(this.form.controls.rol.value);

    // Al escribir un DNI completo se busca si el estudiante ya existe (debounce + switchMap)
    this.form.controls.alumno.controls.dni.valueChanges
      .pipe(
        map((dni) => dni.trim()),
        debounceTime(400),
        distinctUntilChanged(),
        switchMap((dni) =>
          /^\d{8}$/.test(dni)
            ? this.solicitanteService
                .listarPaginado({ page: 0, size: 5, sort: 'apellidos,asc', busqueda: dni })
                .pipe(
                  map((p) => p.content.find((s) => s.dni === dni) ?? null),
                  catchError(() => of(null)),
                )
            : of(null),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((s) => this.mostrarExistente(s));
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.usuarioService.listar().subscribe((data) => this.usuarios.set(data));
  }

  cantidad(filtro: FiltroRol): number {
    return filtro === 'TODOS' ? this.usuarios().length : this.usuarios().filter((u) => u.rol === filtro).length;
  }

  /** ALUMNO: pide los datos del estudiante y no el nombre; otros roles: al revés. */
  private aplicarRol(rol: Rol): void {
    const alumno = this.form.controls.alumno;
    const nombre = this.form.controls.nombre;
    this.esAlumno.set(rol === 'ALUMNO');
    if (rol === 'ALUMNO') {
      alumno.enable({ emitEvent: false });
      nombre.disable({ emitEvent: false });
    } else {
      alumno.disable({ emitEvent: false });
      nombre.enable({ emitEvent: false });
      this.existente.set(null);
      this.avisoDni.set(null);
    }
    this.erroresServidor.set({});
  }

  private mostrarExistente(s: Solicitante | null): void {
    this.existente.set(null);
    this.avisoDni.set(null);
    if (!s) return;
    if (s.tipo !== 'ESTUDIANTE') {
      this.avisoDni.set(`El DNI ${s.dni} pertenece a un solicitante ${s.tipo}; solo se puede vincular un estudiante.`);
      return;
    }
    const conUsuario = this.usuarios().find((u) => u.dni === s.dni);
    if (conUsuario) {
      this.avisoDni.set(`El estudiante ${s.nombres} ${s.apellidos} ya tiene el usuario "${conUsuario.username}".`);
      return;
    }
    this.existente.set(s);
    // Se muestran sus datos (el backend vincula el existente y no los modifica)
    this.form.controls.alumno.patchValue(
      { nombres: s.nombres, apellidos: s.apellidos, email: s.email ?? '', telefono: s.telefono ?? '', area: s.area ?? '' },
      { emitEvent: false },
    );
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const alumno = v.rol === 'ALUMNO';
    const body: RegistroUsuario = {
      username: v.username.trim(),
      password: v.password,
      rol: v.rol,
      nombre: alumno ? '' : v.nombre.trim(),
      alumno: alumno ? { ...v.alumno, dni: v.alumno.dni.trim() } : null,
    };
    this.guardando.set(true);
    this.usuarioService.crear(body).subscribe({
      next: (u) => {
        const vinculo = u.dni ? ` vinculado al estudiante DNI ${u.dni}` : '';
        this.notificar('success', `Usuario "${u.username}" (${u.rol}) registrado${vinculo}`);
        this.erroresServidor.set({});
        this.existente.set(null);
        this.form.reset();
        this.cargar();
        this.guardando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        const e = leerError(err);
        this.erroresServidor.set(e.errores ?? {});
        // 409: no se guardó nada (ni el usuario ni el estudiante): la operación es una sola transacción
        this.notificar('danger', e.status === 409 ? `${e.mensaje}. No se guardó ningún dato.` : e.mensaje);
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

  invalido(campo: 'username' | 'password' | 'nombre'): boolean {
    const c = this.form.controls[campo];
    return (c.touched && c.invalid) || !!this.erroresServidor()[campo];
  }

  invalidoAlumno(campo: CampoAlumno): boolean {
    const c = this.form.controls.alumno.controls[campo];
    return (c.touched && c.invalid) || !!this.erroresServidor()['alumno.' + campo];
  }

  errorAlumno(campo: CampoAlumno, porDefecto: string): string {
    return this.erroresServidor()['alumno.' + campo] || porDefecto;
  }

  private notificar(tipo: 'success' | 'danger', texto: string): void {
    this.mensaje.set({ tipo, texto });
    setTimeout(() => this.mensaje.set(null), 6000);
  }
}
