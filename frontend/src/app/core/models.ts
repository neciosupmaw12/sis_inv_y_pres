export type Rol = 'ADMIN' | 'ENCARGADO';
export type EstadoEquipo = 'OPERATIVO' | 'EN_MANTENIMIENTO' | 'DE_BAJA';
export type TipoSolicitante = 'ESTUDIANTE' | 'DOCENTE' | 'ADMINISTRATIVO';
export type EstadoPrestamo = 'ACTIVO' | 'VENCIDO' | 'DEVUELTO';

export interface Usuario {
  username: string;
  nombre: string;
  rol: Rol;
}

export interface UsuarioSistema extends Usuario {
  id: number;
  activo: boolean;
}

export interface AuthResponse extends Usuario {
  token: string;
  tipo: string;
}

export interface Equipo {
  id?: number;
  codigo: string;
  nombre: string;
  categoria: string;
  marca?: string | null;
  modelo?: string | null;
  descripcion?: string | null;
  cantidadTotal: number;
  cantidadDisponible: number;
  estado: EstadoEquipo;
}

export interface Solicitante {
  id?: number;
  dni: string;
  nombres: string;
  apellidos: string;
  tipo: TipoSolicitante;
  area?: string | null;
  email?: string | null;
  telefono?: string | null;
}

export interface DetallePrestamo {
  equipoId: number;
  codigo: string;
  equipo: string;
  cantidad: number;
}

export interface Prestamo {
  id: number;
  fechaPrestamo: string;
  fechaDevolucionPrevista: string;
  fechaDevolucionReal: string | null;
  estado: EstadoPrestamo;
  solicitanteId: number;
  solicitante: string;
  tipoSolicitante: TipoSolicitante;
  registradoPor: string;
  totalEquipos: number;
  observaciones: string | null;
  detalles: DetallePrestamo[];
}

export interface PrestamoRequest {
  solicitanteId: number;
  fechaDevolucionPrevista: string;
  observaciones: string | null;
  detalles: { equipoId: number; cantidad: number }[];
}

/**
 * Página de resultados: coincide con PageResponse<T> del backend
 * (GET /api/equipos/paginado, /api/prestamos/paginado, /api/solicitantes/paginado).
 */
export interface Pagina<T> {
  content: T[];
  /** Número de página, empieza en 0. */
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  /** Orden aplicado, por ejemplo "nombre,asc". */
  sort: string;
}

/** Parámetros comunes de un listado paginado. */
export interface ConsultaPaginada {
  page: number;
  size: number;
  /** "campo,asc" o "campo,desc" */
  sort: string;
  busqueda?: string;
}

export interface FiltrosEquipo extends ConsultaPaginada {
  estado?: EstadoEquipo | '';
  categoria?: string;
}

export interface FiltrosPrestamo extends ConsultaPaginada {
  estado?: EstadoPrestamo | '';
}

export interface FiltrosSolicitante extends ConsultaPaginada {
  tipo?: TipoSolicitante | '';
}

/** GET /api/prestamos/resumen */
export interface ConteoPrestamos {
  total: number;
  activos: number;
  vencidos: number;
  devueltos: number;
}

/** GET /api/panel/resumen */
export interface PanelResumen {
  equiposPorEstado: { estado: EstadoEquipo; equipos: number; unidadesTotales: number; unidadesDisponibles: number }[];
  unidadesTotales: number;
  unidadesDisponibles: number;
  unidadesPrestadas: number;
  prestamos: ConteoPrestamos;
  topEquipos: { id: number; codigo: string; nombre: string; unidadesPrestadas: number; prestamos: number }[];
}

/** Cuerpo de error que devuelve el GlobalExceptionHandler del backend. */
export interface ApiError {
  status: number;
  error: string;
  mensaje: string;
  errores?: Record<string, string> | null;
}

/** Textos legibles para los valores de los enums. */
export const ETIQUETAS: Record<string, string> = {
  OPERATIVO: 'Operativo',
  EN_MANTENIMIENTO: 'En mantenimiento',
  DE_BAJA: 'De baja',
  ESTUDIANTE: 'Estudiante',
  DOCENTE: 'Docente',
  ADMINISTRATIVO: 'Administrativo',
  ACTIVO: 'Activo',
  VENCIDO: 'Vencido',
  DEVUELTO: 'Devuelto',
  ADMIN: 'Administrador',
  ENCARGADO: 'Encargado',
};
