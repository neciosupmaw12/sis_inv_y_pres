import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Rol, UsuarioSistema } from '../models';

/** Datos del estudiante que se piden al crear un usuario ALUMNO. */
export interface DatosAlumno {
  dni: string;
  nombres: string;
  apellidos: string;
  email: string;
  telefono: string;
  area: string;
}

export interface RegistroUsuario {
  username: string;
  password: string;
  /** Para ADMIN y ENCARGADO; para ALUMNO el backend usa nombres + apellidos del estudiante. */
  nombre: string;
  rol: Rol;
  /** Solo para rol ALUMNO. */
  alumno?: DatosAlumno | null;
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

  /** POST /api/usuarios (para ALUMNO crea o vincula su solicitante en la misma transacción) */
  crear(usuario: RegistroUsuario): Observable<UsuarioSistema> {
    return this.http.post<UsuarioSistema>(this.url, usuario);
  }

  /** DELETE /api/usuarios/{id} (desactivación lógica) */
  desactivar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
