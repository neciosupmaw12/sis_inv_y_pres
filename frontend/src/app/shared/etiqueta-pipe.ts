import { Pipe, PipeTransform } from '@angular/core';
import { ETIQUETAS } from '../core/models';

/** Convierte valores de enum en texto legible: {{ 'EN_MANTENIMIENTO' | etiqueta }} → "En mantenimiento". */
@Pipe({ name: 'etiqueta' })
export class EtiquetaPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    return valor ? (ETIQUETAS[valor] ?? valor) : '';
  }
}
