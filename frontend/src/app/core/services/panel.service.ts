import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PanelResumen } from '../models';

/** Indicadores del panel de inicio calculados en el backend con consultas JPQL de agregación. */
@Injectable({ providedIn: 'root' })
export class PanelService {
  private http = inject(HttpClient);

  /** GET /api/panel/resumen */
  resumen(): Observable<PanelResumen> {
    return this.http.get<PanelResumen>(`${environment.apiUrl}/panel/resumen`);
  }
}
