import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Equipo, FiltrosCatalogo, FiltrosEquipo, Pagina } from '../models';
import { aHttpParams } from './parametros';

/** Administra el inventario de equipos (equivale a "productos"). Consume /api/equipos de Spring Boot. */
@Injectable({ providedIn: 'root' })
export class EquipoService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/equipos`;

  /** GET /api/equipos */
  listar(): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(this.url);
  }

  /** GET /api/equipos/paginado?page=&size=&sort=&busqueda=&estado=&categoria= */
  listarPaginado(filtros: FiltrosEquipo): Observable<Pagina<Equipo>> {
    return this.http.get<Pagina<Equipo>>(`${this.url}/paginado`, { params: aHttpParams({ ...filtros }) });
  }

  /** GET /api/equipos/catalogo?page=&size=&sort=&busqueda=&categoria= (solo OPERATIVOS con stock; lo usa el alumno) */
  catalogo(filtros: FiltrosCatalogo): Observable<Pagina<Equipo>> {
    return this.http.get<Pagina<Equipo>>(`${this.url}/catalogo`, { params: aHttpParams({ ...filtros }) });
  }

  /** GET /api/equipos/disponibles (OPERATIVOS con unidades en almacén) */
  listarDisponibles(): Observable<Equipo[]> {
    return this.http.get<Equipo[]>(`${this.url}/disponibles`);
  }

  /** GET /api/equipos/{id} */
  obtener(id: number): Observable<Equipo> {
    return this.http.get<Equipo>(`${this.url}/${id}`);
  }

  /** POST /api/equipos */
  crear(equipo: Partial<Equipo>): Observable<Equipo> {
    return this.http.post<Equipo>(this.url, equipo);
  }

  /** PUT /api/equipos/{id} */
  actualizar(id: number, equipo: Partial<Equipo>): Observable<Equipo> {
    return this.http.put<Equipo>(`${this.url}/${id}`, equipo);
  }

  /** DELETE /api/equipos/{id} */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
