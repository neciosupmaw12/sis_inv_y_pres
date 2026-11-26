import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Perfil } from '../models';

/** Perfil del alumno autenticado (GET y PUT /api/perfil). */
@Injectable({ providedIn: 'root' })
export class PerfilService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/perfil`;

  obtener(): Observable<Perfil> {
    return this.http.get<Perfil>(this.url);
  }

  /** Solo correo y teléfono; el resto lo cambia el personal en Solicitantes. */
  actualizar(datos: { email: string; telefono: string }): Observable<Perfil> {
    return this.http.put<Perfil>(this.url, datos);
  }
}
