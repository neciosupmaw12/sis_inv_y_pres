import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { EquipoService } from '../../../core/services/equipo.service';
import { PrestamoService } from '../../../core/services/prestamo.service';
import { leerError } from '../../../core/services/http-error';
import { ApiError, Equipo, LIMITES_SOLICITUD } from '../../../core/models';
import { fechaLocal } from '../../../shared/fechas';

type Linea = { equipoId: number | null; cantidad: number };

/** La solicitud completa no puede superar el total de unidades permitido (el backend también lo valida). */
function totalMaximo(control: AbstractControl): ValidationErrors | null {
  const lineas = (control as FormArray).value as Linea[];
  const total = lineas.reduce((suma, l) => suma + (Number(l.cantidad) || 0), 0);
  return total > LIMITES_SOLICITUD.total ? { totalMaximo: { total } } : null;
}

/**
 * Formulario del alumno para solicitar uno o varios equipos (POST /api/prestamos/solicitudes).
 * La solicitud queda PENDIENTE: el stock no se reserva hasta que el ADMIN la aprueba.
 */
@Component({
  selector: 'app-solicitar',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './solicitar.html',
  styleUrl: './solicitar.scss',
})
export class Solicitar implements OnInit {
  private equipoService = inject(EquipoService);
  private prestamoService = inject(PrestamoService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);

  protected readonly limites = LIMITES_SOLICITUD;
  protected readonly hoy = fechaLocal();
  protected readonly equipos = signal<Equipo[]>([]);
  protected readonly cargando = signal(true);
  protected readonly enviando = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  protected readonly form = this.fb.group({
    fechaDevolucionPrevista: [fechaLocal(7), Validators.required],
    motivo: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(255)]],
    detalles: this.fb.array([this.nuevaLinea()], { validators: totalMaximo }),
  });

  private readonly valores = toSignal(this.form.controls.detalles.valueChanges, {
    initialValue: this.form.controls.detalles.value,
  });
  protected readonly total = computed(() =>
    (this.valores() as Linea[]).reduce((suma, l) => suma + (Number(l.cantidad) || 0), 0),
  );

  get lineas(): FormArray {
    return this.form.controls.detalles;
  }

  ngOnInit(): void {
    this.equipoService.listarDisponibles().subscribe({
      next: (lista) => {
        this.equipos.set(lista);
        this.cargando.set(false);
        // Viene del catálogo con ?equipo=ID: se preselecciona
        const elegido = Number(this.route.snapshot.queryParamMap.get('equipo'));
        if (lista.some((e) => e.id === elegido)) {
          this.lineas.at(0).patchValue({ equipoId: elegido });
        }
      },
      error: (err: HttpErrorResponse) => {
        this.error.set(leerError(err));
        this.cargando.set(false);
      },
    });
  }

  nuevaLinea(equipoId: number | null = null) {
    return this.fb.group({
      equipoId: [equipoId, Validators.required],
      cantidad: [1, [Validators.required, Validators.min(1), Validators.max(LIMITES_SOLICITUD.porEquipo)]],
    });
  }

  agregarLinea(): void {
    if (this.lineas.length < LIMITES_SOLICITUD.total) this.lineas.push(this.nuevaLinea());
  }

  quitarLinea(i: number): void {
    if (this.lineas.length > 1) this.lineas.removeAt(i);
  }

  equipo(id: number | null): Equipo | undefined {
    return this.equipos().find((e) => e.id === Number(id));
  }

  /** Un equipo ya elegido en otra línea no se puede repetir (se suma la cantidad en una sola línea). */
  elegidoEnOtraLinea(equipoId: number | undefined, indice: number): boolean {
    return (this.valores() as Linea[]).some((l, i) => i !== indice && Number(l.equipoId) === equipoId);
  }

  /** Máximo por línea: el límite por equipo o las unidades disponibles hoy, lo que sea menor. */
  maximo(equipoId: number | null): number {
    const disponibles = this.equipo(equipoId)?.cantidadDisponible ?? LIMITES_SOLICITUD.porEquipo;
    return Math.min(LIMITES_SOLICITUD.porEquipo, disponibles);
  }

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.enviando.set(true);
    this.error.set(null);
    this.prestamoService
      .solicitar({
        fechaDevolucionPrevista: v.fechaDevolucionPrevista!,
        motivo: v.motivo!.trim(),
        detalles: (v.detalles as Linea[]).map((l) => ({ equipoId: Number(l.equipoId), cantidad: Number(l.cantidad) })),
      })
      .subscribe({
        next: (p) => this.router.navigate(['/mis-prestamos'], { state: { creada: p.id } }),
        error: (err: HttpErrorResponse) => {
          this.error.set(leerError(err));
          this.enviando.set(false);
        },
      });
  }

  erroresDe(e: ApiError): string[] {
    return e.errores ? Object.values(e.errores) : [];
  }
}
