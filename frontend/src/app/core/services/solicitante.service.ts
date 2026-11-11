import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Solicitante } from '../models';

/** Administra los solicitantes (equivale a "clientes"). Consume /api/solicitantes de Spring Boot. */
@Injectable({ providedIn: 'root' })
export class SolicitanteService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/solicitantes`;

  /** GET /api/solicitantes */
  listar(): Observable<Solicitante[]> {
    return this.http.get<Solicitante[]>(this.url);
  }

  /** GET /api/solicitantes/{id} */
  obtener(id: number): Observable<Solicitante> {
    return this.http.get<Solicitante>(`${this.url}/${id}`);
  }

  /** POST /api/solicitantes */
  crear(solicitante: Partial<Solicitante>): Observable<Solicitante> {
    return this.http.post<Solicitante>(this.url, solicitante);
  }

  /** PUT /api/solicitantes/{id} */
  actualizar(id: number, solicitante: Partial<Solicitante>): Observable<Solicitante> {
    return this.http.put<Solicitante>(`${this.url}/${id}`, solicitante);
  }

  /** DELETE /api/solicitantes/{id} */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
