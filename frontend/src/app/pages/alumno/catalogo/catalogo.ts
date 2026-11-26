import { Component, inject, OnInit, signal } from '@angular/core';
import { NgClass } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EquipoService } from '../../../core/services/equipo.service';
import { Equipo, FiltrosCatalogo } from '../../../core/models';
import { ListadoPaginado } from '../../../shared/listado-paginado';
import { Paginacion } from '../../../shared/paginacion/paginacion';

/**
 * Catálogo del alumno: solo equipos OPERATIVOS con stock (GET /api/equipos/catalogo), paginado, con búsqueda
 * (debounce) y filtro de categoría guardados en la URL. Reutiliza ListadoPaginado y el componente de paginación.
 */
@Component({
  selector: 'app-catalogo',
  imports: [ReactiveFormsModule, RouterLink, NgClass, Paginacion],
  templateUrl: './catalogo.html',
  styleUrl: './catalogo.scss',
})
export class Catalogo implements OnInit {
  private equipoService = inject(EquipoService);

  protected readonly listado = new ListadoPaginado<FiltrosCatalogo, Equipo>(
    { page: 0, size: 10, sort: 'nombre,asc', busqueda: '', categoria: '' },
    ['categoria'],
    (consulta) => this.equipoService.catalogo(consulta),
  );

  protected readonly categorias = signal<string[]>([]);
  protected readonly ordenes = [
    { valor: 'nombre,asc', texto: 'Nombre (A→Z)' },
    { valor: 'cantidadDisponible,desc', texto: 'Más unidades disponibles' },
    { valor: 'categoria,asc', texto: 'Categoría' },
    { valor: 'marca,asc', texto: 'Marca' },
  ];

  ngOnInit(): void {
    // Categorías para el filtro: las de los equipos que hoy se pueden pedir
    this.equipoService.listarDisponibles().subscribe((lista) =>
      this.categorias.set([...new Set(lista.map((e) => e.categoria))].sort()),
    );
  }

  claseStock(e: Equipo): string {
    return e.cantidadDisponible <= 2 ? 'text-bg-warning' : 'text-bg-success';
  }

  icono(categoria: string): string {
    const c = categoria.toLowerCase();
    if (c.includes('laptop')) return 'bi-laptop';
    if (c.includes('proyector')) return 'bi-projector';
    if (c.includes('tablet')) return 'bi-tablet';
    if (c.includes('audio')) return 'bi-mic';
    if (c.includes('cámara') || c.includes('perif')) return 'bi-webcam';
    return 'bi-pc-display';
  }
}
