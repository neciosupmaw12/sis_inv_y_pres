# UPN Inventario TI-Gr06

**Sistema web de inventario y préstamo de equipo tecnológico.** API REST en **Spring Boot 4 + Spring Security + JWT** (`upn/`) y frontend en **Angular 21 + Bootstrap 5 + SASS** (`frontend/`).

## Cómo ejecutar

1. Inicia **MySQL** desde XAMPP (usuario `root` sin contraseña). La base `upn_inventario` se crea sola.
2. Backend (puerto 8080), en una terminal PowerShell:
   ```
   cd upn
   .\mvnw spring-boot:run
   ```
3. Frontend (puerto 4200), en otra terminal:
   ```
   cd frontend
   npm.cmd install
   npm.cmd start
   ```
   > Si PowerShell muestra "la ejecución de scripts está deshabilitada", usa `npm.cmd` como arriba
   > o ejecuta una vez `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` para poder usar `npm` directamente.
4. Abre http://localhost:4200

Usuarios creados automáticamente:

| Usuario    | Contraseña    | Rol      |
|------------|---------------|----------|
| `admin`    | `admin123`    | ADMIN    |
| `encargado` | `encargado123` | ENCARGADO |
| `alumno` (Alumno Prueba UPN, estudiante DNI 71234567) | `alumno123` | ALUMNO |
| `lfernandez` (Leslie Fernández Toro) | `upn2026` | ADMIN |
| `rquispe` (Rodolfo Quispe Quispe) | `upn2026` | ADMIN |
| `mneciosup` (Marco Neciosup Panibra) | `upn2026` | ADMIN |
| `ntroncos` (Nelson Raúl Troncos Pintado) | `upn2026` | ADMIN |
| `jmorales` (Juan Carlos Morales Rojas) | `upn2026` | ADMIN |

## Roles y permisos

| Funcionalidad | ADMIN | ENCARGADO | ALUMNO |
|---|:-:|:-:|:-:|
| Panel de inicio con datos globales | ✔ | ✔ | ✘ (ve el resumen de **sus** préstamos) |
| Inventario de equipos: consultar | ✔ | ✔ | solo el **catálogo** (OPERATIVOS con stock) |
| Equipos: registrar, editar, eliminar | ✔ | ✘ | ✘ |
| Solicitantes: consultar, registrar, editar | ✔ | ✔ | ✘ (solo su perfil: correo y teléfono) |
| Solicitantes: eliminar | ✔ | ✘ | ✘ |
| Préstamo directo (nace ACTIVO) y devolución | ✔ | ✔ | ✘ |
| Editar y anular préstamos | ✔ | ✘ | ✘ |
| **Solicitar** un préstamo (queda PENDIENTE) | ✘ | ✘ | ✔ |
| Ver **sus** préstamos y **cancelar** sus solicitudes pendientes | — | — | ✔ |
| **Aprobar / rechazar** solicitudes | ✔ | ✘ (solo las ve en Préstamos) | ✘ |
| Usuarios (crear ADMIN, ENCARGADO o ALUMNO; desactivar) | ✔ | ✘ | ✘ |

Los permisos se validan **en el backend** (reglas por rol en `SecurityConfig` y `@PreAuthorize` en cada controlador);
ocultar botones en Angular es solo comodidad. Cualquier ruta `/api/**` que no tenga una regla explícita queda solo para
ADMIN y ENCARGADO, así que el ALUMNO nunca entra "por descuido". En el frontend, `roleGuard` redirige al inicio de su rol
a quien escriba una URL que no le corresponde (por ejemplo, un alumno que escribe `/usuarios`).

El JWT lleva los claims `rol` y, para el alumno, `solicitanteId`. **El solicitante del alumno se toma siempre del token,
nunca del request**: no puede pedir préstamos a nombre de otro ni ver los ajenos (un préstamo ajeno responde **403**).

### Alta de un alumno (`/usuarios`)

Al elegir el rol **ALUMNO** se piden los datos del estudiante (DNI, nombres, apellidos, correo, teléfono y carrera) y el
usuario queda vinculado a su `Solicitante` de tipo ESTUDIANTE (relación 1 a 1). Si ya existe un solicitante con ese DNI,
se vincula ese en lugar de duplicarlo. Crear el usuario y el estudiante es **una sola transacción**: si algo falla
(usuario, DNI ya vinculado o correo duplicado → **409**), no se guarda ninguno de los dos.

## Flujo de aprobación de préstamos

```
Solicitud del alumno:  PENDIENTE ──aprobar (ADMIN)──▶ ACTIVO ──devolver──▶ DEVUELTO
                         ├──rechazar (ADMIN, motivo obligatorio)──▶ RECHAZADO
                         └──cancelar (el propio alumno)──▶ CANCELADO
Préstamo directo de ADMIN / ENCARGADO: nace ACTIVO (igual que antes).
VENCIDO no se guarda: es un préstamo ACTIVO cuya fecha prevista ya pasó.
```

1. **Solicitar** (`POST /api/prestamos/solicitudes`): queda **PENDIENTE** y **no descuenta stock**. Solo se valida que los
   equipos estén OPERATIVOS, los límites (máx. **3 unidades por equipo**, **5 por solicitud** y **3 solicitudes pendientes**
   por alumno) y que el alumno no tenga préstamos vencidos.
2. **Aprobar** (`PUT /api/prestamos/{id}/aprobar`, solo ADMIN): en **una transacción** vuelve a validar cada equipo,
   descuenta el stock y pasa a **ACTIVO**. Si un equipo ya no tiene unidades suficientes responde **409** y hace
   **ROLLBACK**: no se descuenta nada y la solicitud sigue PENDIENTE. Tampoco se aprueba si la fecha de devolución ya pasó.
3. **Rechazar** (`PUT /api/prestamos/{id}/rechazar` con `{"motivo": "..."}`, solo ADMIN): pasa a **RECHAZADO** sin tocar el
   stock; el alumno ve el motivo en "Mis préstamos".
4. **Cancelar** (`PUT /api/prestamos/{id}/cancelar`, el alumno): solo sus solicitudes y mientras sigan PENDIENTES.

Se guarda quién aprobó o rechazó (`aprobadoPor`), cuándo (`fechaAprobacion`) y el `motivoRechazo`.

## Respaldo de la base de datos

`database/upn_inventario.sql` contiene la estructura y los datos de la base `upn_inventario` (usuarios, equipos, solicitantes y préstamos).
`database/upn_inventario_antes_rol_alumno.sql` es el respaldo tomado **antes** de agregar el rol ALUMNO.

- **Restaurar** (crea la base si no existe y reemplaza sus tablas):
  ```
  C:\xampp\mysql\bin\mysql.exe -uroot < database\upn_inventario.sql
  ```
  También se puede importar desde phpMyAdmin → **Importar**.
- **Generar un nuevo respaldo:**
  ```
  C:\xampp\mysql\bin\mysqldump.exe -uroot --databases upn_inventario --default-character-set=utf8mb4 --skip-dump-date --add-drop-table --routines > database\upn_inventario.sql
  ```

Si no se restaura el respaldo, el backend crea las tablas y los datos iniciales automáticamente al arrancar.

**Migración al agregar el rol ALUMNO.** Los enums ya se guardaban como texto (`EnumType.STRING`, no ORDINAL), pero Hibernate
había creado `usuarios.rol` y `prestamos.estado` como `ENUM` nativo de MySQL, y `ddl-auto=update` no modifica columnas
existentes. Al arrancar, `MigracionEsquema` convierte esas columnas a `VARCHAR(20)` si todavía son ENUM (conserva cada
valor y el índice); en una base nueva no hace nada. Las columnas nuevas (`usuarios.solicitante_id` y los campos de
aprobación de `prestamos`) las agrega Hibernate y quedan en `NULL` para los registros que ya existían.

## Pantallas

| Pantalla | URL | Rol |
|---|---|---|
| Login | http://localhost:4200/login | todos |
| Inicio (indicadores) | http://localhost:4200/inicio | ADMIN, ENCARGADO |
| Inventario de equipos (paginado) | http://localhost:4200/equipos | ADMIN, ENCARGADO |
| Solicitantes | http://localhost:4200/solicitantes | ADMIN, ENCARGADO |
| Préstamos y devoluciones | http://localhost:4200/prestamos | ADMIN, ENCARGADO |
| Solicitudes pendientes (aprobar / rechazar) | http://localhost:4200/solicitudes | ADMIN |
| Usuarios | http://localhost:4200/usuarios | ADMIN |
| Inicio del alumno (resumen de sus préstamos) | http://localhost:4200/mi-inicio | ALUMNO |
| Catálogo de equipos | http://localhost:4200/catalogo | ALUMNO |
| Solicitar préstamo | http://localhost:4200/solicitar | ALUMNO |
| Mis préstamos | http://localhost:4200/mis-prestamos | ALUMNO |
| Mi perfil | http://localhost:4200/mi-perfil | ALUMNO |

Los listados guardan la página, el tamaño, el orden, la búsqueda y los filtros en la URL
(por ejemplo, `/equipos?pagina=2&tamano=5&orden=nombre,desc&q=lap&estado=OPERATIVO`), así que no se pierden al recargar.
El navbar muestra solo las opciones del rol; el del ADMIN tiene un contador de solicitudes pendientes.

## API REST

Todas las rutas (salvo el login) requieren el header `Authorization: Bearer <token>`.

| Método | Ruta                   | Acción  | ADMIN | ENCARGADO | ALUMNO |
|--------|------------------------|---------|:-----:|:--------:|:-----:|
| POST   | `/api/auth/login`      | login   | público | público | público |
| GET    | `/api/equipos[/{id}]`  | index / show | ✔ | ✔ | ✘ |
| POST / PUT / DELETE | `/api/equipos[/{id}]` | create / update / delete | ✔ | ✘ | ✘ |
| GET    | `/api/equipos/paginado` | equipos paginados con búsqueda y filtros | ✔ | ✔ | ✘ |
| GET    | `/api/equipos/disponibles` | equipos OPERATIVOS con stock > 0 | ✔ | ✔ | ✔ |
| GET    | `/api/equipos/catalogo` | lo mismo, paginado con búsqueda y categoría | ✔ | ✔ | ✔ |
| GET / POST / PUT | `/api/solicitantes[/{id}]` | index / show / create / update | ✔ | ✔ | ✘ |
| DELETE | `/api/solicitantes/{id}` | delete | ✔ | ✘ | ✘ |
| GET    | `/api/solicitantes/paginado` | solicitantes paginados | ✔ | ✔ | ✘ |
| GET / POST | `/api/prestamos` | index / préstamo directo (nace ACTIVO) | ✔ | ✔ | ✘ |
| GET    | `/api/prestamos/{id}` | show | ✔ | ✔ | solo los suyos (403 si es ajeno) |
| PUT    | `/api/prestamos/{id}/devolucion` | registrar devolución | ✔ | ✔ | ✘ |
| PUT / DELETE | `/api/prestamos/{id}` | update / delete (anular) | ✔ | ✘ | ✘ |
| GET    | `/api/prestamos/paginado` | préstamos paginados (filtro por estado) | ✔ | ✔ | ✘ |
| GET    | `/api/prestamos/resumen` | cantidad de préstamos por estado | ✔ | ✔ | ✘ |
| GET    | `/api/prestamos/vencidos?solicitanteId=` | préstamos vencidos de un solicitante | ✔ | ✔ | ✘ |
| PUT    | `/api/prestamos/{id}/aprobar` | aprobar solicitud (409 + rollback si no hay stock) | ✔ | ✘ | ✘ |
| PUT    | `/api/prestamos/{id}/rechazar` | rechazar solicitud con motivo | ✔ | ✘ | ✘ |
| POST   | `/api/prestamos/solicitudes` | solicitar préstamo (PENDIENTE) | ✘ | ✘ | ✔ |
| GET    | `/api/prestamos/mis-prestamos` | sus préstamos, paginados | ✘ | ✘ | ✔ |
| GET    | `/api/prestamos/mis-prestamos/resumen` | sus préstamos por estado | ✘ | ✘ | ✔ |
| PUT    | `/api/prestamos/{id}/cancelar` | cancelar su solicitud pendiente | ✘ | ✘ | ✔ |
| GET / PUT | `/api/perfil` | ver su perfil / editar correo y teléfono | ✘ | ✘ | ✔ |
| GET    | `/api/panel/resumen` | indicadores del panel de inicio | ✔ | ✔ | ✘ |
| GET / POST / DELETE | `/api/usuarios[/{id}]` | gestión de usuarios (incluye alta de ALUMNO) | ✔ | ✘ | ✘ |

### Listados paginados

Los endpoints `/paginado`, `/catalogo` y `/mis-prestamos` devuelven un `PageResponse` (no la entidad JPA):

```
GET /api/equipos/paginado?page=0&size=10&sort=nombre,asc&busqueda=lap&estado=OPERATIVO&categoria=Laptops
{ "content": [ ... ], "page": 0, "size": 10, "totalElements": 7, "totalPages": 1,
  "first": true, "last": true, "sort": "nombre,asc" }
```

| Parámetro | Valores | Por defecto |
|---|---|---|
| `page` | 0 o mayor | `0` |
| `size` | 1 a 50 | `10` (catálogo `12`) |
| `sort` | `campo,asc` o `campo,desc` (solo campos permitidos) | equipos `nombre,asc` · solicitantes `apellidos,asc` · préstamos `fechaPrestamo,desc` |
| `busqueda` | texto, sin distinguir mayúsculas | — |
| `estado` | equipos: `OPERATIVO`, `EN_MANTENIMIENTO`, `DE_BAJA` · préstamos: `PENDIENTE`, `ACTIVO`, `VENCIDO`, `DEVUELTO`, `RECHAZADO`, `CANCELADO` | todos |
| `categoria` (equipos y catálogo) · `tipo` (solicitantes) | valor exacto | todos |

Campos de orden permitidos: equipos y catálogo `codigo, nombre, categoria, marca, estado, cantidadTotal, cantidadDisponible`;
solicitantes `apellidos, nombres, dni, tipo, area`; préstamos `id, fechaPrestamo, fechaDevolucionPrevista, estado, solicitante`.
Un valor fuera de rango o un campo no permitido responde **400**.

Los listados completos (`GET /api/equipos`, `/api/solicitantes`, `/api/prestamos`) se mantienen porque los usa el formulario de préstamos.

## Documentación de la API (Swagger / OpenAPI)

Con el backend encendido:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Especificación OpenAPI:** http://localhost:8080/v3/api-docs (JSON) · http://localhost:8080/v3/api-docs.yaml (YAML)
- **Copia en el repositorio:** `docs/openapi.yaml`. Para volver a exportarla:
  ```
  curl.exe -s http://localhost:8080/v3/api-docs.yaml -o docs\openapi.yaml
  ```

Para probar los endpoints protegidos: ejecuta `POST /api/auth/login` con `{"username":"admin","password":"admin123"}`
(o `alumno` / `alumno123` para los endpoints del alumno), copia el `token`, pulsa **Authorize**, pégalo (sin la palabra
*Bearer*) y confirma. Cada respuesta de error (400, 401, 403, 404 y 409) tiene ejemplos reales (`EjemplosError`).
Se usa **springdoc-openapi 3.2.0**, la línea compatible con Spring Boot 4.

## Pruebas automáticas

```
cd upn
.\mvnw test
```

Usan el perfil `test` (`src/test/resources/application-test.properties`) con una base **separada**, `upn_inventario_test`,
que se crea sola: no modifican los datos de `upn_inventario`. El reporte queda en `upn/target/surefire-reports/`. Son 15:

- `PrestamoTransaccionTest`: préstamo exitoso, **rollback** por falta de stock y por equipo no operativo, devolución,
  paginación y bloqueo optimista.
- `SolicitudAlumnoTest` (peticiones HTTP reales con MockMvc y tokens JWT):
  1. el alumno crea una solicitud → PENDIENTE y el stock no cambia;
  2. el admin aprueba → ACTIVO y el stock baja;
  3. aprobación sin stock suficiente → **409 con rollback** (stock y estado sin cambios);
  4. rechazo → RECHAZADO, stock igual y motivo guardado;
  5. el alumno recibe **403** al aprobar, ver o cancelar préstamos ajenos y entrar a `/api/usuarios`;
     y el **encargado** recibe **403** al aprobar o rechazar (la solicitud sigue PENDIENTE y el stock no cambia);
  6. alumno con un DNI ya vinculado → **409** sin usuario ni solicitante nuevos; y si el usuario está duplicado, el
     rollback borra también el estudiante que se había insertado.

## Ver el SQL que genera cada consulta JPQL

En `upn/src/main/resources/application.properties` está activado temporalmente:

```
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.orm.jdbc.bind=TRACE
spring.jpa.properties.hibernate.format_sql=true
```

Para desactivarlo, cambia `DEBUG` y `TRACE` por `INFO` (o borra esas líneas) y reinicia el backend.

### Reglas de negocio

- Un préstamo descuenta unidades disponibles; la devolución (o anulación de un préstamo activo) las repone.
- Solo se prestan equipos en estado `OPERATIVO` y con unidades suficientes.
- La fecha de devolución no puede ser anterior a hoy. Un préstamo activo que pasa esa fecha figura como `VENCIDO`.
- Un solicitante con préstamos vencidos no puede recibir un nuevo préstamo ni enviar solicitudes.
- Las solicitudes de alumnos no tocan el stock hasta que el ADMIN las aprueba; rechazarlas o cancelarlas tampoco.
- Solo se devuelven o editan préstamos entregados (ACTIVO o VENCIDO).
- No se puede reducir la cantidad total de un equipo por debajo de las unidades prestadas.
- No se elimina un equipo o solicitante con préstamos registrados (el equipo se marca `DE_BAJA`), ni un solicitante vinculado a un usuario.
- Registrar, aprobar, editar, devolver o anular un préstamo es **atómico** (`@Transactional`): si un equipo falla, no se guarda nada y no cambia ningún stock.
- `Equipo` tiene **bloqueo optimista** (`@Version`): si dos operaciones modifican el mismo equipo a la vez, la segunda se revierte con **409**.

### Códigos HTTP

| Código | Cuándo |
|--------|--------|
| 200 OK | consulta, actualización, devolución, aprobación, rechazo o cancelación correcta |
| 201 Created | registro creado (incluye header `Location`) |
| 204 No Content | eliminación correcta |
| 400 Bad Request | validación fallida (`errores` por campo), JSON inválido, parámetros de paginación inválidos o regla de negocio incumplida (por ejemplo, aprobar algo que no está PENDIENTE) |
| 401 Unauthorized | sin token, token inválido/expirado o credenciales incorrectas |
| 403 Forbidden | el rol no tiene permiso, o un alumno pide un préstamo ajeno |
| 404 Not Found | el recurso no existe |
| 409 Conflict | código/DNI/email duplicado, estudiante ya vinculado, el registro tiene préstamos, **stock insuficiente** al prestar o aprobar (con rollback) o modificación simultánea del mismo equipo |

## Estructura

```
upn/src/main/java/com/proyecto/upn/
├── config/       SecurityConfig (JWT, roles, CORS), OpenApiConfig (Swagger), EjemplosError,
│                 MigracionEsquema (ENUM → VARCHAR), DataInitializer
├── controller/   Auth, Equipo, Solicitante, Prestamo, Usuario, Panel, Perfil
├── dto/          Requests con validaciones, Responses, PageResponse y proyecciones JPQL
├── entity/       Usuario (↔ Solicitante), Rol, Equipo, Solicitante, Prestamo, DetallePrestamo y enums de estado
├── exception/    GlobalExceptionHandler y excepciones propias (StockInsuficienteException...)
├── repository/   Repositorios JPA con consultas @Query (JPQL)
├── security/     JwtService, JwtAuthenticationFilter, SesionJwt (claims del token), UsuarioDetailsService
├── service/      Lógica de negocio y transacciones
└── util/         Paginacion (page/size/sort con lista blanca)

upn/src/test/     Pruebas JUnit (perfil test, base upn_inventario_test)
docs/             openapi.yaml (especificación exportada)

frontend/src/
├── app/core/     models, services, interceptor JWT, guards (authGuard y roleGuard)
├── app/pages/    login, inicio, equipos, solicitantes, prestamos, solicitudes, usuarios
│   └── alumno/   inicio-alumno, catalogo, solicitar, mis-prestamos, mi-perfil
├── app/shared/   navbar, paginacion, cabecera-orden, listado-paginado, directiva appDisponibilidad, pipe etiqueta
└── styles/       _variables.scss, _mixins.scss (SASS)
```
