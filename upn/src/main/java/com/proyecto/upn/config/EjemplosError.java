package com.proyecto.upn.config;

/**
 * Ejemplos de respuestas de error para la documentación OpenAPI ({@code @ExampleObject}).
 * <p>Son copias de respuestas reales de la API: los errores 400, 404 y 409 los escribe
 * {@code GlobalExceptionHandler} (incluyen {@code errores}); los 401 y 403 los escribe {@code SecurityConfig}
 * antes de llegar al controlador. Al cambiar un mensaje de error, actualiza aquí su ejemplo.
 */
public final class EjemplosError {

    private EjemplosError() {
    }

    // ---------- 400 Bad Request

    public static final String LOGIN_DATOS_INVALIDOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Los datos enviados no son válidos",
              "ruta": "/api/auth/login",
              "errores": {
                "password": "La contraseña es obligatoria"
              }
            }
            """;

    public static final String EQUIPOS_SIZE_INVALIDO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Parámetros no válidos",
              "ruta": "/api/equipos/paginado",
              "errores": {
                "size": "size no puede ser mayor a 50"
              }
            }
            """;

    public static final String EQUIPOS_ORDEN_NO_PERMITIDO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "No se puede ordenar por 'version'. Campos permitidos: marca, categoria, nombre, cantidadDisponible, cantidadTotal, codigo, estado",
              "ruta": "/api/equipos/paginado",
              "errores": null
            }
            """;

    public static final String EQUIPOS_ESTADO_INVALIDO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "El parámetro 'estado' tiene un valor inválido",
              "ruta": "/api/equipos/paginado",
              "errores": null
            }
            """;

    public static final String EQUIPO_DATOS_INVALIDOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Los datos enviados no son válidos",
              "ruta": "/api/equipos",
              "errores": {
                "codigo": "El código es obligatorio",
                "nombre": "El nombre debe tener entre 2 y 100 caracteres",
                "cantidadTotal": "La cantidad total mínima es 1"
              }
            }
            """;

    public static final String EQUIPO_PUT_DATOS_INVALIDOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Los datos enviados no son válidos",
              "ruta": "/api/equipos/5",
              "errores": {
                "categoria": "La categoría es obligatoria"
              }
            }
            """;

    public static final String EQUIPO_CANTIDAD_MENOR_A_PRESTADAS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "La cantidad total no puede ser menor a las 2 unidades que están prestadas",
              "ruta": "/api/equipos/5",
              "errores": null
            }
            """;

    public static final String PRESTAMOS_ESTADO_INVALIDO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Estado no válido: 'XX'. Use ACTIVO, VENCIDO o DEVUELTO",
              "ruta": "/api/prestamos/paginado",
              "errores": null
            }
            """;

    public static final String PRESTAMOS_PAGE_INVALIDO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Parámetros no válidos",
              "ruta": "/api/prestamos/paginado",
              "errores": {
                "page": "page debe ser mayor o igual a 0"
              }
            }
            """;

    public static final String PRESTAMO_DATOS_INVALIDOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Los datos enviados no son válidos",
              "ruta": "/api/prestamos",
              "errores": {
                "fechaDevolucionPrevista": "La fecha de devolución no puede ser anterior a hoy",
                "detalles": "El préstamo debe incluir al menos un equipo"
              }
            }
            """;

    public static final String PRESTAMO_EQUIPO_NO_OPERATIVO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "El equipo 'Tablet Samsung Galaxy Tab' no está operativo (EN_MANTENIMIENTO)",
              "ruta": "/api/prestamos",
              "errores": null
            }
            """;

    public static final String PRESTAMO_SOLICITANTE_CON_VENCIDOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "El solicitante tiene préstamos vencidos (#4); debe devolverlos antes de un nuevo préstamo",
              "ruta": "/api/prestamos",
              "errores": null
            }
            """;

    public static final String PRESTAMO_PUT_DATOS_INVALIDOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Los datos enviados no son válidos",
              "ruta": "/api/prestamos/6",
              "errores": {
                "detalles[0].cantidad": "La cantidad mínima es 1"
              }
            }
            """;

    public static final String PRESTAMO_NO_EDITABLE = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Solo se pueden editar préstamos activos",
              "ruta": "/api/prestamos/2",
              "errores": null
            }
            """;

    public static final String PRESTAMO_YA_DEVUELTO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "El préstamo #2 ya fue devuelto",
              "ruta": "/api/prestamos/2/devolucion",
              "errores": null
            }
            """;

    public static final String SOLICITANTES_PAGE_INVALIDO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Parámetros no válidos",
              "ruta": "/api/solicitantes/paginado",
              "errores": {
                "page": "page debe ser mayor o igual a 0"
              }
            }
            """;

    public static final String SOLICITANTES_ORDEN_NO_PERMITIDO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "No se puede ordenar por 'email'. Campos permitidos: apellidos, nombres, dni, tipo, area",
              "ruta": "/api/solicitantes/paginado",
              "errores": null
            }
            """;

    public static final String SOLICITANTE_DATOS_INVALIDOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Los datos enviados no son válidos",
              "ruta": "/api/solicitantes",
              "errores": {
                "dni": "El DNI debe tener exactamente 8 dígitos",
                "email": "El email no tiene un formato válido"
              }
            }
            """;

    public static final String SOLICITANTE_PUT_DATOS_INVALIDOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Los datos enviados no son válidos",
              "ruta": "/api/solicitantes/2",
              "errores": {
                "nombres": "Los nombres deben tener entre 2 y 80 caracteres"
              }
            }
            """;

    public static final String USUARIO_DATOS_INVALIDOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 400,
              "error": "Bad Request",
              "mensaje": "Los datos enviados no son válidos",
              "ruta": "/api/usuarios",
              "errores": {
                "nombre": "El nombre es obligatorio"
              }
            }
            """;

    // ---------- 401 Unauthorized

    public static final String LOGIN_CREDENCIALES_INCORRECTAS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 401,
              "error": "Unauthorized",
              "mensaje": "Usuario o contraseña incorrectos",
              "ruta": "/api/auth/login",
              "errores": null
            }
            """;

    public static final String EQUIPOS_SIN_TOKEN = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 401,
              "error": "Unauthorized",
              "mensaje": "Debe iniciar sesión para acceder a este recurso",
              "ruta": "/api/equipos"
            }
            """;

    public static final String EQUIPOS_TOKEN_EXPIRADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 401,
              "error": "Unauthorized",
              "mensaje": "El token ha expirado",
              "ruta": "/api/equipos"
            }
            """;

    public static final String EQUIPOS_PAGINADO_SIN_TOKEN = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 401,
              "error": "Unauthorized",
              "mensaje": "Debe iniciar sesión para acceder a este recurso",
              "ruta": "/api/equipos/paginado"
            }
            """;

    public static final String EQUIPOS_PAGINADO_TOKEN_INVALIDO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 401,
              "error": "Unauthorized",
              "mensaje": "Token inválido",
              "ruta": "/api/equipos/paginado"
            }
            """;

    public static final String PANEL_SIN_TOKEN = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 401,
              "error": "Unauthorized",
              "mensaje": "Debe iniciar sesión para acceder a este recurso",
              "ruta": "/api/panel/resumen"
            }
            """;

    public static final String PRESTAMOS_SIN_TOKEN = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 401,
              "error": "Unauthorized",
              "mensaje": "Debe iniciar sesión para acceder a este recurso",
              "ruta": "/api/prestamos"
            }
            """;

    // ---------- 403 Forbidden

    public static final String EQUIPOS_SIN_PERMISO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 403,
              "error": "Forbidden",
              "mensaje": "No tiene permisos para realizar esta operación",
              "ruta": "/api/equipos"
            }
            """;

    public static final String EQUIPO_SIN_PERMISO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 403,
              "error": "Forbidden",
              "mensaje": "No tiene permisos para realizar esta operación",
              "ruta": "/api/equipos/5"
            }
            """;

    public static final String PRESTAMO_SIN_PERMISO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 403,
              "error": "Forbidden",
              "mensaje": "No tiene permisos para realizar esta operación",
              "ruta": "/api/prestamos/6"
            }
            """;

    public static final String SOLICITANTE_SIN_PERMISO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 403,
              "error": "Forbidden",
              "mensaje": "No tiene permisos para realizar esta operación",
              "ruta": "/api/solicitantes/3"
            }
            """;

    public static final String USUARIOS_SIN_PERMISO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 403,
              "error": "Forbidden",
              "mensaje": "No tiene permisos para realizar esta operación",
              "ruta": "/api/usuarios"
            }
            """;

    // ---------- 404 Not Found

    public static final String EQUIPO_NO_ENCONTRADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 404,
              "error": "Not Found",
              "mensaje": "Equipo con id 999 no encontrado",
              "ruta": "/api/equipos/999",
              "errores": null
            }
            """;

    public static final String PRESTAMO_NO_ENCONTRADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 404,
              "error": "Not Found",
              "mensaje": "Préstamo con id 999 no encontrado",
              "ruta": "/api/prestamos/999",
              "errores": null
            }
            """;

    public static final String DEVOLUCION_PRESTAMO_NO_ENCONTRADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 404,
              "error": "Not Found",
              "mensaje": "Préstamo con id 999 no encontrado",
              "ruta": "/api/prestamos/999/devolucion",
              "errores": null
            }
            """;

    public static final String VENCIDOS_SOLICITANTE_NO_ENCONTRADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 404,
              "error": "Not Found",
              "mensaje": "Solicitante con id 999 no encontrado",
              "ruta": "/api/prestamos/vencidos",
              "errores": null
            }
            """;

    public static final String PRESTAMO_SOLICITANTE_NO_ENCONTRADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 404,
              "error": "Not Found",
              "mensaje": "Solicitante con id 999 no encontrado",
              "ruta": "/api/prestamos",
              "errores": null
            }
            """;

    public static final String PRESTAMO_EQUIPO_NO_ENCONTRADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 404,
              "error": "Not Found",
              "mensaje": "Equipo con id 999 no encontrado",
              "ruta": "/api/prestamos",
              "errores": null
            }
            """;

    public static final String SOLICITANTE_NO_ENCONTRADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 404,
              "error": "Not Found",
              "mensaje": "Solicitante con id 999 no encontrado",
              "ruta": "/api/solicitantes/999",
              "errores": null
            }
            """;

    public static final String USUARIO_NO_ENCONTRADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 404,
              "error": "Not Found",
              "mensaje": "Usuario con id 999 no encontrado",
              "ruta": "/api/usuarios/999",
              "errores": null
            }
            """;

    // ---------- 409 Conflict

    public static final String EQUIPO_CODIGO_DUPLICADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "Ya existe un equipo con el código LAP-001",
              "ruta": "/api/equipos",
              "errores": null
            }
            """;

    public static final String EQUIPO_PUT_CODIGO_DUPLICADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "Ya existe otro equipo con el código LAP-001",
              "ruta": "/api/equipos/5",
              "errores": null
            }
            """;

    public static final String EQUIPO_MODIFICACION_SIMULTANEA = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "Otro usuario modificó el mismo equipo al mismo tiempo. No se guardó ningún cambio; vuelva a intentarlo.",
              "ruta": "/api/equipos/5",
              "errores": null
            }
            """;

    public static final String EQUIPO_CON_PRESTAMOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "No se puede eliminar el equipo porque tiene préstamos registrados; cambie su estado a DE_BAJA",
              "ruta": "/api/equipos/5",
              "errores": null
            }
            """;

    public static final String PRESTAMO_STOCK_INSUFICIENTE = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "No hay unidades suficientes de 'Proyector multimedia Epson' (PRY-001). Disponibles: 5, solicitadas: 50. No se registró el préstamo.",
              "ruta": "/api/prestamos",
              "errores": {
                "equipoId": "3",
                "codigo": "PRY-001",
                "disponibles": "5",
                "solicitadas": "50"
              }
            }
            """;

    public static final String PRESTAMO_MODIFICACION_SIMULTANEA = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "Otro usuario modificó el mismo equipo al mismo tiempo. No se guardó ningún cambio; vuelva a intentarlo.",
              "ruta": "/api/prestamos",
              "errores": null
            }
            """;

    public static final String PRESTAMO_PUT_STOCK_INSUFICIENTE = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "No hay unidades suficientes de 'Proyector multimedia Epson' (PRY-001). Disponibles: 5, solicitadas: 50. No se registró el préstamo.",
              "ruta": "/api/prestamos/6",
              "errores": {
                "equipoId": "3",
                "codigo": "PRY-001",
                "disponibles": "5",
                "solicitadas": "50"
              }
            }
            """;

    public static final String DEVOLUCION_MODIFICACION_SIMULTANEA = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "Otro usuario modificó el mismo equipo al mismo tiempo. No se guardó ningún cambio; vuelva a intentarlo.",
              "ruta": "/api/prestamos/6/devolucion",
              "errores": null
            }
            """;

    public static final String SOLICITANTE_DNI_DUPLICADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "Ya existe un solicitante con el DNI 70123456",
              "ruta": "/api/solicitantes",
              "errores": null
            }
            """;

    public static final String SOLICITANTE_EMAIL_DUPLICADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "Ya existe un solicitante con el email maria.quispe@upn.pe",
              "ruta": "/api/solicitantes",
              "errores": null
            }
            """;

    public static final String SOLICITANTE_PUT_DNI_DUPLICADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "Ya existe otro solicitante con el DNI 70123456",
              "ruta": "/api/solicitantes/2",
              "errores": null
            }
            """;

    public static final String SOLICITANTE_PUT_EMAIL_DUPLICADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "Ya existe otro solicitante con el email maria.quispe@upn.pe",
              "ruta": "/api/solicitantes/2",
              "errores": null
            }
            """;

    public static final String SOLICITANTE_CON_PRESTAMOS = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "No se puede eliminar el solicitante porque tiene préstamos registrados",
              "ruta": "/api/solicitantes/1",
              "errores": null
            }
            """;

    public static final String USUARIO_DUPLICADO = """
            {
              "timestamp": "2026-11-25T09:45:18",
              "status": 409,
              "error": "Conflict",
              "mensaje": "El usuario admin ya existe",
              "ruta": "/api/usuarios",
              "errores": null
            }
            """;

}
