import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Rol, UsuarioSistema } from '../models';

export interface RegistroUsuario {
  username: string;
  password: string;
  nombre: string;
  rol: Rol;
}

/** Administra los usuarios del sistema (solo ADMIN). Consume /api/usuarios de Spring Boot. */
@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/usuarios`;

  /** GET /api/usuarios */
  listar(): Observable<UsuarioSistema[]> {
    return this.http.get<UsuarioSistema[]>(this.url);
  }

  /** POST /api/usuarios */
  crear(usuario: RegistroUsuario): Observable<UsuarioSistema> {
    return this.http.post<UsuarioSistema>(this.url, usuario);
  }

  /** DELETE /api/usuarios/{id} (desactivación lógica) */
  desactivar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
