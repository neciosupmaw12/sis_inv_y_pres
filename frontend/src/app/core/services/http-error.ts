import { HttpErrorResponse } from '@angular/common/http';
import { ApiError } from '../models';

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
