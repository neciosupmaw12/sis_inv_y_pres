import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ConteoPrestamos, FiltrosPrestamo, Pagina, Prestamo, PrestamoRequest, SolicitudRequest } from '../models';
import { aHttpParams } from './parametros';

/** Administra los préstamos y devoluciones (equivale a "ventas"). Consume /api/prestamos de Spring Boot. */
@Injectable({ providedIn: 'root' })
export class PrestamoService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/prestamos`;

  /** GET /api/prestamos */
  listar(): Observable<Prestamo[]> {
    return this.http.get<Prestamo[]>(this.url);
  }

  /** GET /api/prestamos/paginado?page=&size=&sort=&estado=&busqueda= */
  listarPaginado(filtros: FiltrosPrestamo): Observable<Pagina<Prestamo>> {
    return this.http.get<Pagina<Prestamo>>(`${this.url}/paginado`, { params: aHttpParams({ ...filtros }) });
  }

  /** GET /api/prestamos/resumen (cantidad por estado; incluye las solicitudes pendientes) */
  resumen(): Observable<ConteoPrestamos> {
    return this.http.get<ConteoPrestamos>(`${this.url}/resumen`);
  }

  /** GET /api/prestamos/{id} */
  obtener(id: number): Observable<Prestamo> {
    return this.http.get<Prestamo>(`${this.url}/${id}`);
  }

  /** POST /api/prestamos (préstamo directo de ADMIN o ENCARGADO: nace ACTIVO) */
  crear(prestamo: PrestamoRequest): Observable<Prestamo> {
    return this.http.post<Prestamo>(this.url, prestamo);
  }

  /** PUT /api/prestamos/{id} */
  actualizar(id: number, prestamo: PrestamoRequest): Observable<Prestamo> {
    return this.http.put<Prestamo>(`${this.url}/${id}`, prestamo);
  }

  /** PUT /api/prestamos/{id}/devolucion */
  devolver(id: number): Observable<Prestamo> {
    return this.http.put<Prestamo>(`${this.url}/${id}/devolucion`, null);
  }

  /** DELETE /api/prestamos/{id} */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  // ---------- ADMIN: solicitudes de alumnos

  /** PUT /api/prestamos/{id}/aprobar → ACTIVO y descuenta stock; 409 si ya no alcanza (rollback) */
  aprobar(id: number): Observable<Prestamo> {
    return this.http.put<Prestamo>(`${this.url}/${id}/aprobar`, null);
  }

  /** PUT /api/prestamos/{id}/rechazar → RECHAZADO con motivo; el stock no se toca */
  rechazar(id: number, motivo: string): Observable<Prestamo> {
    return this.http.put<Prestamo>(`${this.url}/${id}/rechazar`, { motivo });
  }

  // ---------- ALUMNO (el solicitante sale del JWT: no se envía)

  /** POST /api/prestamos/solicitudes → PENDIENTE, sin descontar stock */
  solicitar(solicitud: SolicitudRequest): Observable<Prestamo> {
    return this.http.post<Prestamo>(`${this.url}/solicitudes`, solicitud);
  }

  /** GET /api/prestamos/mis-prestamos?page=&size=&sort=&estado= */
  misPrestamos(filtros: FiltrosPrestamo): Observable<Pagina<Prestamo>> {
    return this.http.get<Pagina<Prestamo>>(`${this.url}/mis-prestamos`, { params: aHttpParams({ ...filtros }) });
  }

  /** GET /api/prestamos/mis-prestamos/resumen */
  misResumen(): Observable<ConteoPrestamos> {
    return this.http.get<ConteoPrestamos>(`${this.url}/mis-prestamos/resumen`);
  }

  /** PUT /api/prestamos/{id}/cancelar (solo las propias y PENDIENTES) */
  cancelar(id: number): Observable<Prestamo> {
    return this.http.put<Prestamo>(`${this.url}/${id}/cancelar`, null);
  }
}
