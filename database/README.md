# Respaldos de la base de datos `upn_inventario`

Volcados de MariaDB 10.4 (XAMPP) generados con `mysqldump`, en **UTF-8**. Cada archivo crea la base `upn_inventario`
si no existe y **reemplaza sus tablas** (`DROP TABLE IF EXISTS`). Contienen solo **datos de prueba**; las contraseñas de
los usuarios están encriptadas con **BCrypt** (`$2a$10$…`), nunca en texto plano.

## Archivos

| Archivo | Fecha del volcado | Estado de la base | equipos | solicitantes | préstamos | usuarios |
|---|---|---|:-:|:-:|:-:|:-:|
| `upn_inventario.sql` | 26/11/2026 | **Actual** (rol ALUMNO y flujo de aprobación). `usuarios.rol` y `prestamos.estado` en `VARCHAR(20)`, `usuarios.solicitante_id` (usuario ↔ estudiante), campos de aprobación en `prestamos` (`fecha_solicitud`, `aprobado_por_id`, `fecha_aprobacion`, `motivo_rechazo`) y el usuario `alumno` / `alumno123`. | 7 | 5 | 4 | 8 |
| `upn_inventario_paginacion.sql` | 26/11/2026 15:35 | Con **paginación, `@Version` e índices**, sin el rol ALUMNO: columna `equipos.version`, 8 índices `idx_*`, `rol` y `estado` todavía `ENUM`. Mismo estado que el archivo siguiente, regenerado con `--result-file` (ver nota). | 7 | 4 | 4 | 7 |
| `upn_inventario_antes_rol_alumno.sql` | 26/11/2026 (antes de empezar el rol ALUMNO) | El mismo estado de `upn_inventario_paginacion.sql`: es el respaldo que se sacó de la base real justo antes de agregar el rol ALUMNO. | 7 | 4 | 4 | 7 |
| `upn_inventario_antes_paginacion.sql` | 24/11/2026 22:44 | **Antes de la paginación**: sin columna `equipos.version` ni índices `idx_*`; `rol` y `estado` en `ENUM`. | 7 | 4 | 4 | 7 |

Los conteos se comprobaron importando cada archivo en una base temporal (`upn_verificacion`) que luego se eliminó.

**Nota sobre `upn_inventario_paginacion.sql`.** Cuando se generó, la base real ya había sido migrada al rol ALUMNO
(el backend la migra solo al arrancar), así que no podía volcarse "sin la migración". Se cargó
`upn_inventario_antes_rol_alumno.sql` en una base temporal, se volcó con `mysqldump --result-file` y se dejó el nombre
`upn_inventario`. Los datos y la estructura son idénticos a `upn_inventario_antes_rol_alumno.sql`; solo cambia la línea
final con la fecha del volcado.

**¿Cuál usar?** Con el código actual, `upn_inventario.sql`. Si se importa uno anterior, el backend lo actualiza al
arrancar: Hibernate (`ddl-auto=update`) agrega las columnas e índices que falten, `MigracionEsquema` pasa `rol` y
`estado` a `VARCHAR(20)` y `DataInitializer` crea el usuario `alumno`.

## Cómo importar un respaldo

> ⚠️ Importar **reemplaza las tablas de `upn_inventario`** y se pierden los datos que tenga. Si los necesitas, saca antes un
> respaldo (ver más abajo). Conviene detener el backend antes de importar y volver a arrancarlo después.

Desde la carpeta del proyecto (`ProyectoUPN`):

- **CMD (Símbolo del sistema):**
  ```
  C:\xampp\mysql\bin\mysql.exe -uroot < database\upn_inventario.sql
  ```
- **PowerShell** (no admite el operador `<`; usa `source`):
  ```
  C:\xampp\mysql\bin\mysql.exe -uroot -e "source database/upn_inventario.sql"
  ```
- **phpMyAdmin:** abre http://localhost/phpmyadmin → pestaña **Importar** → **Seleccionar archivo** (por ejemplo,
  `database/upn_inventario.sql`) → juego de caracteres **utf-8** → **Importar**. No hace falta elegir una base a la
  izquierda: el archivo crea y usa `upn_inventario`.

Para revisarlo sin tocar `upn_inventario`, importa una copia en la que cambies `` `upn_inventario` `` por otro nombre
(por ejemplo, `` `upn_verificacion` ``) en las líneas `CREATE DATABASE` y `USE`.

## Cómo sacar un respaldo nuevo

```
C:\xampp\mysql\bin\mysqldump.exe -uroot --databases upn_inventario --add-drop-table --routines --default-character-set=utf8mb4 --result-file=database\upn_inventario.sql
```

Usa `--result-file` y **no** la redirección `>` de PowerShell: PowerShell guarda la salida en UTF-16 y después el archivo
no se puede importar. Agrega `--skip-dump-date` si quieres que dos volcados de la misma base sean idénticos (sin la línea
con la fecha).
