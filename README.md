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

## Pantallas

| Pantalla | URL |
|---|---|
| Login | http://localhost:4200/login |
| Inicio (indicadores) | http://localhost:4200/inicio |
| Inventario de equipos | http://localhost:4200/equipos |
| Solicitantes | http://localhost:4200/solicitantes |
| Préstamos y devoluciones | http://localhost:4200/prestamos |
| Usuarios (solo ADMIN) | http://localhost:4200/usuarios |

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

### Reglas de negocio

- Un préstamo descuenta unidades disponibles; la devolución (o anulación de un préstamo activo) las repone.
- Solo se prestan equipos en estado `OPERATIVO` y con unidades suficientes.
- La fecha de devolución no puede ser anterior a hoy. Un préstamo activo que pasa esa fecha figura como `VENCIDO`.
- Un solicitante con préstamos vencidos no puede recibir un nuevo préstamo.
- No se puede reducir la cantidad total de un equipo por debajo de las unidades prestadas.
- No se elimina un equipo o solicitante con préstamos registrados (el equipo se marca `DE_BAJA`).

### Códigos HTTP

| Código | Cuándo |
|--------|--------|
| 200 OK | consulta, actualización o devolución correcta |
| 201 Created | registro creado (incluye header `Location`) |
| 204 No Content | eliminación correcta |
| 400 Bad Request | validación fallida (`errores` por campo), JSON inválido o regla de negocio incumplida |
| 401 Unauthorized | sin token, token inválido/expirado o credenciales incorrectas |
| 403 Forbidden | el rol no tiene permiso |
| 404 Not Found | el recurso no existe |
| 409 Conflict | código/DNI/email duplicado, o el registro tiene préstamos |

## Estructura

```
upn/src/main/java/com/proyecto/upn/
├── config/       SecurityConfig (JWT, roles, CORS), DataInitializer
├── controller/   Auth, Equipo, Solicitante, Prestamo, Usuario
├── dto/          Requests con validaciones y Responses
├── entity/       Usuario, Rol, Equipo, Solicitante, Prestamo, DetallePrestamo y enums de estado
├── exception/    GlobalExceptionHandler y excepciones propias
├── repository/   Repositorios JPA
├── security/     JwtService, JwtAuthenticationFilter, UsuarioDetailsService
└── service/      Lógica de negocio

frontend/src/
├── app/core/     models, services (auth, api), interceptor JWT, guard
├── app/pages/    login, inicio, equipos, solicitantes, prestamos, usuarios
├── app/shared/   navbar, directiva appDisponibilidad, pipe etiqueta
└── styles/       _variables.scss, _mixins.scss (SASS)
```
