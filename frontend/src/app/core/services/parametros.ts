import { HttpParams } from '@angular/common/http';
import { ConsultaPaginada } from '../models';

/**
 * Convierte la consulta de un listado paginado en HttpParams (?page=0&size=10&sort=nombre,asc&...).
 * Omite los filtros vacíos para que el backend los ignore.
 */
export function aHttpParams(consulta: ConsultaPaginada & Record<string, unknown>): HttpParams {
  let params = new HttpParams();
  for (const [clave, valor] of Object.entries(consulta)) {
    if (valor === null || valor === undefined || valor === '') continue;
    params = params.set(clave, String(valor).trim());
  }
  return params;
}
