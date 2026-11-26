export type Rol = 'ADMIN' | 'ENCARGADO' | 'ALUMNO';
export type EstadoEquipo = 'OPERATIVO' | 'EN_MANTENIMIENTO' | 'DE_BAJA';
export type TipoSolicitante = 'ESTUDIANTE' | 'DOCENTE' | 'ADMINISTRATIVO';
/** VENCIDO no se guarda: es un préstamo ACTIVO cuya fecha prevista ya pasó (lo calcula el backend). */
export type EstadoPrestamo = 'PENDIENTE' | 'ACTIVO' | 'VENCIDO' | 'DEVUELTO' | 'RECHAZADO' | 'CANCELADO';

export interface Usuario {
  username: string;
  nombre: string;
  rol: Rol;
  /** Solo ALUMNO: su solicitante (el backend lo toma del JWT, no de aquí). */
  solicitanteId?: number | null;
}

export interface UsuarioSistema extends Usuario {
  id: number;
  activo: boolean;
  /** Solo ALUMNO: estudiante vinculado. */
  dni: string | null;
  carrera: string | null;
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
  /** Stock actual del equipo (para decidir si se puede aprobar una solicitud). */
  disponibles: number;
  estadoEquipo: EstadoEquipo;
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
  /** Observaciones del préstamo directo o motivo de la solicitud del alumno. */
  observaciones: string | null;
  /** Solo solicitudes de alumnos. */
  fechaSolicitud: string | null;
  aprobadoPor: string | null;
  fechaAprobacion: string | null;
  motivoRechazo: string | null;
  detalles: DetallePrestamo[];
}

export interface PrestamoRequest {
  solicitanteId: number;
  fechaDevolucionPrevista: string;
  observaciones: string | null;
  detalles: { equipoId: number; cantidad: number }[];
}

/** POST /api/prestamos/solicitudes (sin solicitanteId: el backend lo toma del JWT). */
export interface SolicitudRequest {
  fechaDevolucionPrevista: string;
  motivo: string;
  detalles: { equipoId: number; cantidad: number }[];
}

/** Límites de una solicitud de alumno (los mismos que valida el backend). */
export const LIMITES_SOLICITUD = { porEquipo: 3, total: 5, pendientes: 3 } as const;

/** GET /api/perfil */
export interface Perfil {
  username: string;
  solicitanteId: number;
  dni: string;
  nombres: string;
  apellidos: string;
  tipo: TipoSolicitante;
  carrera: string | null;
  email: string | null;
  telefono: string | null;
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

/** GET /api/equipos/catalogo (solo OPERATIVOS con stock). */
export interface FiltrosCatalogo extends ConsultaPaginada {
  categoria?: string;
}

export interface FiltrosPrestamo extends ConsultaPaginada {
  estado?: EstadoPrestamo | '';
}

export interface FiltrosSolicitante extends ConsultaPaginada {
  tipo?: TipoSolicitante | '';
}

/** GET /api/prestamos/resumen y /api/prestamos/mis-prestamos/resumen */
export interface ConteoPrestamos {
  total: number;
  activos: number;
  vencidos: number;
  devueltos: number;
  pendientes: number;
  rechazados: number;
  cancelados: number;
}

/** Cantidad de un estado dentro del conteo (para las pestañas de filtro). */
export function contarEstado(c: ConteoPrestamos, estado: EstadoPrestamo | 'TODOS'): number {
  const mapa: Record<EstadoPrestamo | 'TODOS', number> = {
    TODOS: c.total,
    PENDIENTE: c.pendientes,
    ACTIVO: c.activos,
    VENCIDO: c.vencidos,
    DEVUELTO: c.devueltos,
    RECHAZADO: c.rechazados,
    CANCELADO: c.cancelados,
  };
  return mapa[estado];
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
  PENDIENTE: 'Pendiente',
  ACTIVO: 'Activo',
  VENCIDO: 'Vencido',
  DEVUELTO: 'Devuelto',
  RECHAZADO: 'Rechazado',
  CANCELADO: 'Cancelado',
  ADMIN: 'Administrador',
  ENCARGADO: 'Encargado',
  ALUMNO: 'Alumno',
};

/**
 * Badge de cada estado de préstamo: PENDIENTE amarillo, ACTIVO azul, VENCIDO naranja, DEVUELTO verde,
 * RECHAZADO rojo y CANCELADO gris.
 */
export const CLASE_ESTADO_PRESTAMO: Record<EstadoPrestamo, string> = {
  PENDIENTE: 'text-bg-warning',
  ACTIVO: 'text-bg-primary',
  VENCIDO: 'text-bg-vencido',
  DEVUELTO: 'text-bg-success',
  RECHAZADO: 'text-bg-danger',
  CANCELADO: 'text-bg-secondary',
};

/** Badge de cada rol: ADMIN amarillo, ENCARGADO celeste y ALUMNO morado. */
export const CLASE_ROL: Record<Rol, string> = {
  ADMIN: 'text-bg-warning',
  ENCARGADO: 'text-bg-info',
  ALUMNO: 'text-bg-alumno',
};
