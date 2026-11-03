import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiError } from '../models';

export type Recurso = 'equipos' | 'solicitantes' | 'prestamos' | 'usuarios';

/** Servicio CRUD genérico para los endpoints index/show/create/update/delete del backend. */
@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  index<T>(recurso: Recurso): Observable<T[]> {
    return this.http.get<T[]>(`${this.base}/${recurso}`);
  }

  show<T>(recurso: Recurso, id: number): Observable<T> {
    return this.http.get<T>(`${this.base}/${recurso}/${id}`);
  }

  create<T>(recurso: Recurso, body: unknown): Observable<T> {
    return this.http.post<T>(`${this.base}/${recurso}`, body);
  }

  update<T>(recurso: Recurso, id: number, body: unknown): Observable<T> {
    return this.http.put<T>(`${this.base}/${recurso}/${id}`, body);
  }

  delete(recurso: Recurso, id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${recurso}/${id}`);
  }

  /** PUT sobre una acción del recurso, p. ej. /prestamos/5/devolucion */
  accion<T>(recurso: Recurso, id: number, accion: string): Observable<T> {
    return this.http.put<T>(`${this.base}/${recurso}/${id}/${accion}`, null);
  }
}

/** Extrae el mensaje y los errores por campo de una respuesta de error del backend. */
export function leerError(err: HttpErrorResponse): ApiError {
  if (err.status === 0) {
    return { status: 0, error: 'Sin conexión', mensaje: 'No se pudo conectar con el servidor' };
  }
  const body = err.error as Partial<ApiError> | null;
  return {
    status: err.status,
    error: body?.error ?? err.statusText,
    mensaje: body?.mensaje ?? 'Ocurrió un error inesperado',
    errores: body?.errores ?? null,
  };
}
