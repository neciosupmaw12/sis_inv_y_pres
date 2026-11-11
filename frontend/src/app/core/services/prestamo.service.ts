import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Prestamo, PrestamoRequest } from '../models';

/** Administra los préstamos y devoluciones (equivale a "ventas"). Consume /api/prestamos de Spring Boot. */
@Injectable({ providedIn: 'root' })
export class PrestamoService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/prestamos`;

  /** GET /api/prestamos */
  listar(): Observable<Prestamo[]> {
    return this.http.get<Prestamo[]>(this.url);
  }

  /** GET /api/prestamos/{id} */
  obtener(id: number): Observable<Prestamo> {
    return this.http.get<Prestamo>(`${this.url}/${id}`);
  }

  /** POST /api/prestamos */
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
}
