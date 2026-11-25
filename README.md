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
| `lfernandez` (Leslie Fernández Toro) | `upn2026` | ADMIN |
| `rquispe` (Rodolfo Quispe Quispe) | `upn2026` | ADMIN |
| `mneciosup` (Marco Neciosup Panibra) | `upn2026` | ADMIN |
| `ntroncos` (Nelson Raúl Troncos Pintado) | `upn2026` | ADMIN |
| `jmorales` (Juan Carlos Morales Rojas) | `upn2026` | ADMIN |

## Respaldo de la base de datos

`database/upn_inventario.sql` contiene la estructura y los datos de la base `upn_inventario` (usuarios, equipos, solicitantes y préstamos).

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

## Pantallas

| Pantalla | URL |
|---|---|
| Login | http://localhost:4200/login |
| Inicio (indicadores) | http://localhost:4200/inicio |
| Inventario de equipos (paginado) | http://localhost:4200/equipos |
| Solicitantes | http://localhost:4200/solicitantes |
| Préstamos y devoluciones | http://localhost:4200/prestamos |
| Usuarios (solo ADMIN) | http://localhost:4200/usuarios |

Los listados guardan la página, el tamaño, el orden, la búsqueda y los filtros en la URL
(por ejemplo, `/equipos?pagina=2&tamano=5&orden=nombre,desc&q=lap&estado=OPERATIVO`), así que no se pierden al recargar.

## API REST

Todas las rutas (salvo el login) requieren el header `Authorization: Bearer <token>`.

| Método | Ruta                   | Acción  | ADMIN | ENCARGADO |
|--------|------------------------|---------|:-----:|:--------:|
| POST   | `/api/auth/login`      | login   | público | público |
| GET    | `/api/equipos[/{id}]`  | index / show | ✔ | ✔ |
| POST / PUT / DELETE | `/api/equipos[/{id}]` | create / update / delete | ✔ | ✘ |
| GET / POST / PUT | `/api/solicitantes[/{id}]` | index / show / create / update | ✔ | ✔ |
| DELETE | `/api/solicitantes/{id}` | delete | ✔ | ✘ |
| GET / POST | `/api/prestamos[/{id}]` | index / show / create | ✔ | ✔ |
| PUT    | `/api/prestamos/{id}/devolucion` | registrar devolución | ✔ | ✔ |
| PUT / DELETE | `/api/prestamos/{id}` | update / delete (anular) | ✔ | ✘ |
| GET / POST / DELETE | `/api/usuarios[/{id}]` | gestión de usuarios | ✔ | ✘ |
| GET    | `/api/equipos/paginado` | equipos paginados con búsqueda y filtros | ✔ | ✔ |
| GET    | `/api/equipos/disponibles` | equipos OPERATIVOS con stock > 0 | ✔ | ✔ |
| GET    | `/api/solicitantes/paginado` | solicitantes paginados | ✔ | ✔ |
| GET    | `/api/prestamos/paginado` | préstamos paginados (filtro por estado) | ✔ | ✔ |
| GET    | `/api/prestamos/resumen` | cantidad de préstamos por estado | ✔ | ✔ |
| GET    | `/api/prestamos/vencidos?solicitanteId=` | préstamos vencidos de un solicitante | ✔ | ✔ |
| GET    | `/api/panel/resumen` | indicadores del panel de inicio | ✔ | ✔ |

### Listados paginados

Los endpoints `/paginado` devuelven un `PageResponse` (no la entidad JPA):

```
GET /api/equipos/paginado?page=0&size=10&sort=nombre,asc&busqueda=lap&estado=OPERATIVO&categoria=Laptops
{ "content": [ ... ], "page": 0, "size": 10, "totalElements": 7, "totalPages": 1,
  "first": true, "last": true, "sort": "nombre,asc" }
```

| Parámetro | Valores | Por defecto |
|---|---|---|
| `page` | 0 o mayor | `0` |
| `size` | 1 a 50 | `10` |
| `sort` | `campo,asc` o `campo,desc` (solo campos permitidos) | equipos `nombre,asc` · solicitantes `apellidos,asc` · préstamos `fechaPrestamo,desc` |
| `busqueda` | texto, sin distinguir mayúsculas | — |
| `estado` | equipos: `OPERATIVO`, `EN_MANTENIMIENTO`, `DE_BAJA` · préstamos: `ACTIVO`, `VENCIDO`, `DEVUELTO` | todos |
| `categoria` (equipos) · `tipo` (solicitantes) | valor exacto | todos |

Campos de orden permitidos: equipos `codigo, nombre, categoria, marca, estado, cantidadTotal, cantidadDisponible`;
solicitantes `apellidos, nombres, dni, tipo, area`; préstamos `id, fechaPrestamo, fechaDevolucionPrevista, estado, solicitante`.
Un valor fuera de rango o un campo no permitido responde **400**.

Los listados completos (`GET /api/equipos`, `/api/solicitantes`, `/api/prestamos`) se mantienen porque los usa el formulario de préstamos.

## Documentación de la API (Swagger / OpenAPI)

Con el backend encendido:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Especificación OpenAPI (JSON):** http://localhost:8080/v3/api-docs

Para probar los endpoints protegidos: ejecuta `POST /api/auth/login` con `{"username":"admin","password":"admin123"}`,
copia el `token`, pulsa **Authorize**, pégalo (sin la palabra *Bearer*) y confirma.
Se usa **springdoc-openapi 3.2.0**, la línea compatible con Spring Boot 4.

## Pruebas automáticas

```
cd upn
.\mvnw test
```

Usan el perfil `test` (`src/test/resources/application-test.properties`) con una base **separada**, `upn_inventario_test`,
que se crea sola: no modifican los datos de `upn_inventario`. Verifican el préstamo exitoso, el **rollback** por falta de stock
y por equipo no operativo, la devolución, la paginación y el bloqueo optimista. El reporte queda en `upn/target/surefire-reports/`.

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
- Un solicitante con préstamos vencidos no puede recibir un nuevo préstamo.
- No se puede reducir la cantidad total de un equipo por debajo de las unidades prestadas.
- No se elimina un equipo o solicitante con préstamos registrados (el equipo se marca `DE_BAJA`).
- Registrar, editar, devolver o anular un préstamo es **atómico** (`@Transactional`): si un equipo falla, no se guarda nada y no cambia ningún stock.
- `Equipo` tiene **bloqueo optimista** (`@Version`): si dos operaciones modifican el mismo equipo a la vez, la segunda se revierte con **409**.

### Códigos HTTP

| Código | Cuándo |
|--------|--------|
| 200 OK | consulta, actualización o devolución correcta |
| 201 Created | registro creado (incluye header `Location`) |
| 204 No Content | eliminación correcta |
| 400 Bad Request | validación fallida (`errores` por campo), JSON inválido, parámetros de paginación inválidos o regla de negocio incumplida |
| 401 Unauthorized | sin token, token inválido/expirado o credenciales incorrectas |
| 403 Forbidden | el rol no tiene permiso |
| 404 Not Found | el recurso no existe |
| 409 Conflict | código/DNI/email duplicado, el registro tiene préstamos, **stock insuficiente** en un préstamo (con rollback) o modificación simultánea del mismo equipo |

## Estructura

```
upn/src/main/java/com/proyecto/upn/
├── config/       SecurityConfig (JWT, roles, CORS), OpenApiConfig (Swagger), DataInitializer
├── controller/   Auth, Equipo, Solicitante, Prestamo, Usuario, Panel
├── dto/          Requests con validaciones, Responses, PageResponse y proyecciones JPQL
├── entity/       Usuario, Rol, Equipo, Solicitante, Prestamo, DetallePrestamo y enums de estado
├── exception/    GlobalExceptionHandler y excepciones propias (StockInsuficienteException...)
├── repository/   Repositorios JPA con consultas @Query (JPQL)
├── security/     JwtService, JwtAuthenticationFilter, UsuarioDetailsService
├── service/      Lógica de negocio y transacciones
└── util/         Paginacion (page/size/sort con lista blanca)

upn/src/test/     Pruebas JUnit (perfil test, base upn_inventario_test)

frontend/src/
├── app/core/     models, services (auth, equipo, solicitante, prestamo, usuario, panel), interceptor JWT, guard
├── app/pages/    login, inicio, equipos, solicitantes, prestamos, usuarios
├── app/shared/   navbar, paginacion, cabecera-orden, listado-paginado, directiva appDisponibilidad, pipe etiqueta
└── styles/       _variables.scss, _mixins.scss (SASS)
```
