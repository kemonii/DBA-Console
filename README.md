# DBA Console

Herramienta web para administrar Oracle Database 21c Express Edition.

## Descripción general

DBA Console reúne en una sola aplicación las tareas habituales de administración de Oracle Database 21c XE: respaldos, tunning de consultas, seguridad, auditoría, tablespaces y archivos de respaldo. Está dirigida a administradores de bases de datos que gestionan instancias XE. Se compone de una API REST en Spring Boot y de una interfaz en Angular, ubicada en [`frontend/`](frontend), que muestra las pantallas en español.

## Módulos

| Módulo | Descripción | Estado |
|---|---|---|
| Respaldos y recuperación | Exportaciones con Data Pump (`expdp`) por tabla, esquema o base de datos completa; recuperación con `impdp` y Flashback. | Disponible |
| Tunning y performance | Planes de ejecución, estadísticas del optimizador y mantenimiento de índices; monitoreo de la instancia, las sesiones y los bloqueos. | Disponible |
| Seguridad | Usuarios, roles, privilegios y perfiles; auditoría; administración de tablespaces y datafiles. | Disponible |
| Administración de archivos | Objetos directorio de Oracle y los archivos de respaldo y de log que contienen. | Disponible |

Además de los endpoints de cada módulo, descritos más abajo, la API ofrece la comprobación de conexión que se explica en [Puesta en marcha](#puesta-en-marcha).

## Tecnologías

- Java 17
- Spring Boot 3.5 (Web, JDBC, Validation)
- Spring `JdbcTemplate` para el acceso a datos (sin JPA ni Hibernate: la aplicación trabaja con vistas del diccionario de datos y DDL de Oracle, donde el mapeo de entidades no aporta nada)
- Controlador JDBC de Oracle (`ojdbc11`)
- Utilidades de línea de comandos de Oracle Data Pump (`expdp`, `impdp`) para los respaldos lógicos
- springdoc-openapi para la documentación OpenAPI y Swagger UI
- JUnit 5 para las pruebas
- Maven
- Angular 22 (componentes standalone, signals, Signal Forms) con Vitest para el frontend

## Requisitos previos

- JDK 17 o superior
- Oracle Database 21c XE
- Node.js 22.22.3+, 24.15+ o 26+ (las versiones impares 23 y 25 no son compatibles) y npm (solo para el frontend)

No hace falta instalar Maven: Maven Wrapper (`mvnw` / `mvnw.cmd`) descarga la versión que usa el proyecto la primera vez que se ejecuta.

La aplicación debe ejecutarse en el mismo equipo que la base de datos. Los respaldos invocan las utilidades `expdp` e `impdp`, que deben estar disponibles en el `PATH`, y los archivos de respaldo y de log que generan se escriben en el sistema de archivos del servidor de la base de datos.

## Configuración

La aplicación lee las siguientes variables. Ninguna tiene valor por defecto, y las credenciales nunca deben escribirse en un archivo que se suba al repositorio.

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_URL` | URL JDBC del servicio de la base de datos conectable | `jdbc:oracle:thin:@//localhost:1521/XEPDB1` |
| `DB_USER` | Usuario de la base de datos con privilegios administrativos | `your_admin_user` |
| `DB_PASS` | Contraseña de `DB_USER` | `your_password` |
| `DATAFILE_DIR` | Directorio donde se crean los datafiles nuevos | `/opt/oracle/oradata/XE/XEPDB1` |

`DB_URL` debe apuntar al servicio `XEPDB1` (la base de datos conectable), no al contenedor raíz de XE. Los usuarios normales, sin el prefijo `C##`, solo se pueden crear dentro de una base de datos conectable; en el contenedor raíz solo se pueden crear usuarios comunes.

`DB_USER` necesita privilegios administrativos, porque la aplicación lee vistas `DBA_*` y `V$`, ejecuta DDL y realiza exportaciones completas de la base de datos. En una instalación local basta con conectarse como `SYSTEM` a `XEPDB1`.

La ubicación de los datafiles cambia entre instalaciones de Windows y Linux; por eso `DATAFILE_DIR` es configurable.

### Archivo `.env` (recomendado)

Copie [`.env.example`](.env.example) como `.env` en la raíz del proyecto y complete sus valores. La aplicación lo lee cuando se inicia desde la raíz del proyecto, y Git ignora `.env`.

```powershell
Copy-Item .env.example .env
```

`.env` se lee como un archivo de propiedades de Java, así que escriba las rutas de Windows con barras normales, por ejemplo `DATAFILE_DIR=C:/app/oracle/oradata/XE/XEPDB1`. La barra invertida inicia una secuencia de escape.

### Variables de entorno

Las variables de entorno tienen prioridad sobre `.env`, así que también se pueden definir en la terminal, por ejemplo en un equipo sin archivo `.env`.

PowerShell:

```powershell
$env:DB_URL = "jdbc:oracle:thin:@//localhost:1521/XEPDB1"
$env:DB_USER = "your_admin_user"
$env:DB_PASS = "your_password"
$env:DATAFILE_DIR = "C:\app\oracle\oradata\XE\XEPDB1"
```

CMD:

```bat
set DB_URL=jdbc:oracle:thin:@//localhost:1521/XEPDB1
set DB_USER=your_admin_user
set DB_PASS=your_password
set DATAFILE_DIR=C:\app\oracle\oradata\XE\XEPDB1
```

bash:

```bash
export DB_URL="jdbc:oracle:thin:@//localhost:1521/XEPDB1"
export DB_USER="your_admin_user"
export DB_PASS="your_password"
export DATAFILE_DIR="/opt/oracle/oradata/XE/XEPDB1"
```

## Puesta en marcha

1. Clone el repositorio:

   ```bash
   git clone https://github.com/kemonii/DBA-Console.git
   cd DBA-Console
   ```

2. Cree el archivo `.env` o defina las variables de entorno descritas en [Configuración](#configuración).

3. Inicie la aplicación desde la raíz del proyecto:

   ```powershell
   .\mvnw.cmd spring-boot:run   # Windows
   ```

   ```bash
   ./mvnw spring-boot:run       # Linux y macOS
   ```

4. Abra la documentación de la API en <http://localhost:8080/swagger-ui.html>.

5. Verifique la conexión con la base de datos:

   ```bash
   curl http://localhost:8080/api/health
   ```

6. Inicie el frontend en una segunda terminal (el backend debe estar en ejecución):

   ```bash
   cd frontend
   npm install
   npm start
   ```

   Abra <http://localhost:4200>. El servidor de desarrollo redirige cada solicitud a `/api` al backend en el puerto 8080 (consulte `frontend/proxy.conf.json`), así que no hace falta configurar CORS. El encabezado indica si el backend puede conectarse con la base de datos.

7. Antes de hacer un respaldo, cree el objeto directorio en **Archivos → Directorios**. La pantalla de respaldos propone el nombre `RESPALDO`, que se puede cambiar.

Para compilar y ejecutar las pruebas del backend (no requieren base de datos):

```bash
./mvnw verify   # .\mvnw.cmd verify en Windows
```

Para compilar y ejecutar las pruebas del frontend:

```bash
cd frontend
npm test -- --no-watch
npm run build
```

## Estructura del proyecto

```
src/main/java/una/dbadmin
├── DbAdminApplication.java
├── common/                    Infraestructura compartida
│   ├── OperationResult.java
│   ├── GlobalExceptionHandler.java
│   ├── HealthController.java
│   └── OracleIdentifiers.java
├── backup/                    Respaldos y recuperación
├── tuning/
│   ├── queries/               Planes de ejecución, estadísticas, índices
│   └── monitoring/            Instancia, sesiones, bloqueos
├── security/
│   ├── tablespaces/           Administración de tablespaces
│   ├── auditing/              Auditoría
│   └── users/                 Administración de usuarios
└── files/                     Administración de archivos de respaldo

frontend/src/app
├── core/                      Cliente de la API, modelo OperationResult, monitor de conexión
├── layout/                    Estructura de la aplicación, menú de navegación, estado de conexión
├── backup/                    Pantallas y rutas de respaldos y recuperación
├── tuning/                    Pantallas y rutas de tunning y performance
├── security/                  Pantallas y rutas de seguridad
├── files/                     Pantallas y rutas de administración de archivos
└── example/                   Pantalla de referencia (formulario, llamada a la API, resultado)
```

El frontend sigue la misma idea. Cada carpeta de módulo en `frontend/src/app` contiene sus pantallas y un archivo `<módulo>.routes.ts` que la aplicación carga de forma diferida, de modo que cada persona edita solo su carpeta. Para crear una pantalla, copie el patrón de `example/` y llame a la API mediante `ApiClient`, en `core/`, que devuelve los campos de `OperationResult` descritos abajo. La pantalla de ejemplo no aparece en el menú, pero sigue disponible en `/example`.

El código del backend se organiza por módulo y no por capa. Cada funcionalidad vive en su propio paquete con su controlador y su servicio, así que quienes trabajan en módulos distintos rara vez tocan los mismos archivos. El código que comparten todos los módulos va en `common/`.

## Convenciones de la API

Todos los endpoints devuelven un `OperationResult` con los siguientes campos:

| Campo | Tipo | Descripción |
|---|---|---|
| `ok` | boolean | `true` si la operación tuvo éxito |
| `message` | string | Descripción del resultado, legible para las personas |
| `data` | any | Datos de la respuesta, o `null` si no hay |

Respuesta exitosa:

```json
{
  "ok": true,
  "message": "La conexión con la base de datos está disponible",
  "data": null
}
```

Los errores usan el mismo formato, con `ok` en `false`. Una entrada no válida devuelve HTTP 400. Los errores de base de datos devuelven HTTP 500 y conservan el código de error de Oracle cuando existe. Estos errores llegan como los entrega Oracle, en el idioma configurado en la base de datos:

```json
{
  "ok": false,
  "message": "ORA-00942: table or view does not exist",
  "data": null
}
```

## Endpoints de respaldos y recuperación

Los nombres de esquemas, tablas y directorios se validan como identificadores de Oracle. Los esquemas completos de cada solicitud y respuesta están en Swagger UI.

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/backup` | Ejecuta `expdp`. Cuerpo: `{"type": "SCHEMA", "schema": "HR", "directory": "RESPALDO", "fileName": "hr"}`. `type` puede ser `TABLE` (con `table`), `SCHEMA` o `FULL` |
| `GET` | `/api/backup/log?directory=...&fileName=...` | Devuelve hasta las últimas 500 líneas de un archivo `.log` |
| `POST` | `/api/recovery` | Ejecuta `impdp`. Cuerpo: `{"type": "SCHEMA", "schema": "HR", "directory": "RESPALDO", "fileName": "hr"}`. `FULL` exige `"confirmFull": true`, porque sobrescribe toda la base de datos |
| `POST` | `/api/flashback/table` | `FLASHBACK TABLE ... TO BEFORE DROP`. Cuerpo: `{"schema": "HR", "table": "JOBS"}` |
| `POST` | `/api/flashback/query` | Consulta las filas de una tabla en un momento anterior (`AS OF TIMESTAMP`). Cuerpo: `{"schema": "HR", "table": "JOBS", "timestamp": "2026-10-26T10:15:00"}` |
| `POST` | `/api/flashback/restore` | Inserta las filas que existían en ese momento y ahora faltan. Usa el mismo cuerpo que `/query` |

En los respaldos `SCHEMA` y `FULL`, `includeTables` exporta solo las tablas indicadas y `excludeTables` exporta todas menos esas. No se pueden usar juntos.

## Endpoints de administración de archivos

Todas las rutas empiezan por `/api/files`. Los nombres de directorios y usuarios se validan como identificadores de Oracle. Los esquemas completos de cada solicitud y respuesta están en Swagger UI.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/directories` | Lista los objetos directorio (`DBA_DIRECTORIES`) |
| `POST` | `/directories` | Crea o reemplaza un objeto directorio. Cuerpo: `{"name": "BACKUPS", "path": "/u01/backups"}` |
| `DELETE` | `/directories/{name}` | Elimina un objeto directorio |
| `POST` | `/directories/{name}/grants` | Otorga `READ, WRITE` sobre el directorio a un usuario. Cuerpo: `{"user": "SCOTT"}` |
| `DELETE` | `/directories/{name}/grants/{user}` | Revoca `READ, WRITE` sobre el directorio a un usuario |
| `GET` | `/directories/{name}/files` | Lista los archivos `.dmp` y `.log` del directorio, con su tamaño y fecha de modificación |
| `GET` | `/directories/{name}/files/{file}/content` | Muestra un archivo `.log` (hasta 1000 líneas y 256 KB; `truncated` indica si hay más) |
| `DELETE` | `/directories/{name}/files/{file}?confirm=true` | Elimina un archivo de respaldo; sin `confirm=true` la solicitud se rechaza |

Los nombres de archivo deben ser nombres simples terminados en `.dmp` o `.log`, formados por letras, dígitos, `_`, `.` y `-`. Los nombres con separadores de ruta, `..` u otros caracteres se rechazan con HTTP 400, y la ruta resuelta (incluidos los enlaces simbólicos) debe permanecer dentro de la ruta del directorio. Los archivos se leen desde la ruta del directorio en el equipo donde se ejecuta la aplicación.

## Endpoints de tablespaces

Todas las rutas empiezan por `/api/security/tablespaces` y usan las sentencias del curso (clase 4). Los nombres de tablespaces se validan como identificadores de Oracle. Los esquemas completos de cada solicitud y respuesta están en Swagger UI.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/` | Lista los tablespaces con su espacio total, usado y libre en MB (script de uso de la clase 4: `DBA_DATA_FILES` y `DBA_FREE_SPACE`) |
| `POST` | `/` | `CREATE TABLESPACE ... DATAFILE ... SIZE ... AUTOEXTEND ON NEXT ... MAXSIZE ...`. Cuerpo: `{"name": "VENTAS", "fileName": "ventas01.dbf", "sizeMb": 2, "nextMb": 1, "maxSizeMb": 1024}`; omita `maxSizeMb` para usar `UNLIMITED` |
| `DELETE` | `/{name}?confirm=true` | `DROP TABLESPACE ... INCLUDING CONTENTS AND DATAFILES`; sin `confirm=true` la solicitud se rechaza, y `SYSTEM` y `SYSAUX` siempre se rechazan |
| `GET` | `/{name}/datafiles` | Lista los datafiles de un tablespace (`DBA_DATA_FILES`) |
| `POST` | `/{name}/datafiles` | `ALTER TABLESPACE ... ADD DATAFILE ... SIZE ...`. Cuerpo: `{"fileName": "ventas02.dbf", "sizeMb": 256}` |
| `POST` | `/{name}/datafiles/{fileId}/resize` | `ALTER DATABASE DATAFILE ... RESIZE ...`. Cuerpo: `{"sizeMb": 5}` |

Los datafiles nuevos siempre se crean en `DATAFILE_DIR`: la solicitud indica solo un nombre de archivo simple terminado en `.dbf`, y los nombres con separadores de ruta se rechazan con HTTP 400. Los tamaños son megabytes enteros entre 1 y 32767. El espacio libre se mide dentro del tamaño asignado, así que los datafiles autoextensibles aún pueden crecer; los tablespaces temporales no se listan.

## Endpoints de usuarios, roles, privilegios y perfiles

Todas las rutas empiezan por `/api/security` y usan las sentencias del curso ("Seguridad"). Los nombres se validan como identificadores de Oracle. Los usuarios y roles que mantiene Oracle (`ORACLE_MAINTAINED = 'Y'`, como `SYS`, `SYSTEM` o `DBA`) nunca se bloquean, se eliminan ni pierden privilegios.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/users` | Lista los usuarios creados por administradores (`DBA_USERS`) |
| `POST` | `/users` | `CREATE USER ... IDENTIFIED BY "..." [DEFAULT TABLESPACE ...] [TEMPORARY TABLESPACE ...] [QUOTA ... ON ...]`. Cuerpo: `{"username": "PRUEBA", "password": "...", "defaultTablespace": "USERS", "temporaryTablespace": "TEMP", "quotaMb": 50}`; `"unlimitedQuota": true` para `QUOTA UNLIMITED` |
| `POST` | `/users/{name}/lock` | `ALTER USER ... ACCOUNT LOCK` |
| `POST` | `/users/{name}/unlock` | `ALTER USER ... ACCOUNT UNLOCK` |
| `DELETE` | `/users/{name}?confirm=true` | `DROP USER ...`; agregue `cascade=true` para eliminar también los objetos del usuario |
| `GET` | `/roles` | Lista los roles (`DBA_ROLES`), primero los creados por administradores |
| `POST` | `/roles` | `CREATE ROLE ...`. Cuerpo: `{"name": "VENTAS_ROL"}` |
| `DELETE` | `/roles/{name}` | `DROP ROLE ...` |
| `GET` | `/grants/system-privileges` | Lista los privilegios de sistema que se pueden otorgar (`SYSTEM_PRIVILEGE_MAP`) |
| `GET` | `/grants/{grantee}` | Privilegios de sistema, roles y privilegios sobre objetos de un usuario o rol (`DBA_SYS_PRIVS`, `DBA_ROLE_PRIVS`, `DBA_TAB_PRIVS`) |
| `POST` | `/grants/system` | `GRANT <privilege> TO <grantee> [WITH ADMIN OPTION]`. Cuerpo: `{"privilege": "CREATE SESSION", "grantee": "PRUEBA", "adminOption": false}` |
| `DELETE` | `/grants/system?privilege=...&grantee=...` | `REVOKE <privilege> FROM <grantee>` |
| `POST` | `/grants/role` | `GRANT <role> TO <grantee> [WITH ADMIN OPTION]`. Cuerpo: `{"role": "CONNECT", "grantee": "PRUEBA"}` |
| `DELETE` | `/grants/role?role=...&grantee=...` | `REVOKE <role> FROM <grantee>` |
| `POST` | `/grants/object` | `GRANT <privileges> ON <owner>.<object> TO <grantee> [WITH GRANT OPTION]`. Cuerpo: `{"privileges": ["SELECT", "INSERT"], "owner": "HR", "object": "EMPLOYEES", "grantee": "PRUEBA"}` |
| `DELETE` | `/grants/object?privileges=SELECT,INSERT&owner=...&object=...&grantee=...` | `REVOKE <privileges> ON <owner>.<object> FROM <grantee>` |
| `GET` | `/profiles` | Perfiles y sus límites de recursos y contraseñas (`DBA_PROFILES`) |
| `POST` | `/profiles` | `CREATE PROFILE ... LIMIT ...`. Cuerpo: `{"name": "APP_USER", "limits": {"SESSIONS_PER_USER": "2", "IDLE_TIME": "30", "FAILED_LOGIN_ATTEMPTS": "3"}}` |
| `PUT` | `/profiles/{name}` | `ALTER PROFILE ... LIMIT ...`, por ejemplo `{"limits": {"IDLE_TIME": "20"}}` sobre `DEFAULT` |
| `DELETE` | `/profiles/{name}` | `DROP PROFILE ... CASCADE` (sus usuarios pasan a `DEFAULT`); `DEFAULT` se rechaza |
| `PUT` | `/users/{name}/profile` | `ALTER USER ... PROFILE ...`. Cuerpo: `{"profile": "APP_USER"}` |

Los límites de perfil son `SESSIONS_PER_USER`, `CPU_PER_SESSION`, `CPU_PER_CALL`, `CONNECT_TIME`, `IDLE_TIME`, `LOGICAL_READS_PER_SESSION`, `LOGICAL_READS_PER_CALL`, `FAILED_LOGIN_ATTEMPTS`, `PASSWORD_LIFE_TIME`, `PASSWORD_REUSE_TIME`, `PASSWORD_REUSE_MAX`, `PASSWORD_LOCK_TIME` y `PASSWORD_GRACE_TIME`; cada uno admite un número entero, `UNLIMITED` o `DEFAULT`. El beneficiario puede ser un usuario, un rol o `PUBLIC`. Las contraseñas no pueden enviarse como parámetros enlazados, así que se rechazan si contienen comillas dobles o caracteres de control, y se escriben entre comillas dobles. Los nombres de privilegios de sistema se comprueban contra `SYSTEM_PRIVILEGE_MAP`, y los privilegios sobre objetos contra la lista del curso (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, `ALTER`, `INDEX`, `REFERENCES`, `EXECUTE`, `DEBUG`, `FLASHBACK`, `READ`, `WRITE`, `ALL`).

## Endpoints de auditoría

Todas las rutas empiezan por `/api/security/auditing` y usan las sentencias y vistas del curso ("Auditoría Oracle"). Se trata de la auditoría tradicional: los registros se escriben en `SYS.AUD$` solo mientras el parámetro `audit_trail` sea `DB` (o `DB, EXTENDED`). La aplicación solo lee el parámetro, porque cambiarlo requiere `ALTER SYSTEM SET audit_trail = DB SCOPE = SPFILE` y reiniciar la base de datos.

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/parameter` | Valor de `audit_trail` (`V$PARAMETER`) e indicación de si escribe en la base de datos |
| `GET` | `/options` | Opciones de sentencias (`DBA_STMT_AUDIT_OPTS`) y tablas auditadas (`DBA_OBJ_AUDIT_OPTS`, sin los esquemas propios de Oracle) |
| `POST` | `/session` | `AUDIT SESSION [WHENEVER [NOT] SUCCESSFUL]`. Cuerpo: `{"whenever": "ALWAYS"}`, `"SUCCESSFUL"` o `"NOT_SUCCESSFUL"` |
| `DELETE` | `/session?whenever=ALWAYS` | `NOAUDIT SESSION [WHENEVER ...]` |
| `POST` | `/tables` | `AUDIT <actions> ON <owner>.<table> BY ACCESS \| BY SESSION`. Cuerpo: `{"owner": "HR", "table": "JOBS", "actions": ["INSERT", "UPDATE", "DELETE"], "byAccess": true}` |
| `DELETE` | `/tables?owner=...&table=...&actions=INSERT,UPDATE` | `NOAUDIT <actions> ON <owner>.<table>` |
| `GET` | `/trail` | Filas más recientes de `DBA_AUDIT_TRAIL` |
| `GET` | `/trail/sessions` | Intentos de inicio de sesión más recientes de `DBA_AUDIT_SESSION`, con el resultado decodificado como en clase (`Connected`, `Failed`, ...) |
| `GET` | `/trail/objects` | Acciones auditadas más recientes sobre objetos, de `DBA_AUDIT_OBJECT` |

Los tres endpoints `/trail` aceptan los parámetros opcionales `username` y `limit` (100 por defecto, 1000 como máximo), ambos enviados como valores enlazados. Las acciones sobre tablas son `INSERT`, `UPDATE`, `DELETE` y `SELECT`.

## Endpoints de tunning y performance

El frontend, en `/tuning`, tiene las pantallas de plan de ejecución, estadísticas e índices, y monitoreo. Todas las respuestas usan `OperationResult`, y los errores de Oracle conservan su código ORA. Los nombres de objetos se validan con `OracleIdentifiers.requireValid`. Las columnas de los índices son nombres simples de columna, sin expresiones SQL.

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/tuning/queries/plan` | Explica una sentencia; cuerpo `{"statement":"SELECT * FROM HR.JOBS"}` |
| GET | `/api/tuning/queries/statistics/{schema}` | NUM_ROWS y LAST_ANALYZED de las tablas |
| POST | `/api/tuning/queries/statistics/{schema}/gather` | Recopila las estadísticas del esquema |
| POST | `/api/tuning/queries/statistics/{schema}/{table}/gather` | Recopila las estadísticas de la tabla |
| GET | `/api/tuning/queries/indexes/{schema}` | Lista los índices |
| POST | `/api/tuning/queries/indexes` | Crea un índice; cuerpo `{"schema":"HR","table":"JOBS","name":"JOBS_IDX","columns":["JOB_ID"],"type":"BTREE"}`; el tipo también puede ser `BITMAP` |
| POST | `/api/tuning/queries/indexes/{schema}/{index}/rebuild` | Reconstruye el índice |
| POST | `/api/tuning/queries/indexes/{schema}/{index}/validate` | Valida la estructura y lee INDEX_STATS |
| DELETE | `/api/tuning/queries/indexes/{schema}/{index}?confirm=true` | Elimina el índice |
| GET | `/api/tuning/monitoring/instance` | Estado de la instancia |
| GET | `/api/tuning/monitoring/parameters` | Parámetros principales del sistema |
| GET | `/api/tuning/monitoring/sessions` | Sesiones actuales, cursores abiertos y sesión bloqueadora |
| GET | `/api/tuning/monitoring/sessions/programs` | Cantidad de sesiones por programa |
| GET | `/api/tuning/monitoring/sessions/users` | Cantidad de sesiones por usuario |
| GET | `/api/tuning/monitoring/locks` | Sesiones bloqueadoras y en espera, de V$LOCK y V$SESSION |
| GET | `/api/tuning/monitoring/locks/objects` | Objetos bloqueados por sesiones |
| DELETE | `/api/tuning/monitoring/sessions/{sid}/{serial}?confirm=true` | Termina una sesión solo si el SID y el serial siguen identificando a una sesión bloqueadora |

EXPLAIN PLAN y su lectura jerárquica con CONNECT BY se ejecutan dentro de un mismo `ConnectionCallback`, con un identificador de sentencia único, y al final se eliminan solo las filas de ese plan. ANALYZE INDEX e INDEX_STATS también comparten una conexión. Las estimaciones no son tiempos reales de ejecución. Los cursores abiertos usan la estadística de sesión `opened cursors current`, lo que evita contar entradas de cursores en caché.

El administrador configurado necesita acceso a DBA_TABLES, DBA_INDEXES, DBA_OBJECTS, las vistas V$, PLAN_TABLE e INDEX_STATS; permiso de ejecución sobre DBMS_STATS; y los privilegios CREATE/ALTER/DROP INDEX, ANALYZE ANY y ALTER SYSTEM correspondientes a los objetos que administra. La base de datos informa las restricciones de edición, tipo de objeto y propiedad. La validación usa VALIDATE STRUCTURE fuera de línea y puede mantener bloqueos mientras se ejecuta. Terminar una sesión revierte su trabajo sin confirmar; ambas operaciones destructivas tienen un paso de confirmación explícito en la pantalla.

### Verificación manual en Oracle 21c XE

Use un esquema de aplicación y una tabla desechables, con el backend configurado para XEPDB1:

1. Explique un SELECT sobre la tabla de prueba y verifique la jerarquía y el resaltado de los recorridos completos. Explique un SELECT no válido y verifique que aparezca el error de Oracle; repita la prueba para comprobar las conexiones del pool.
2. Recopile las estadísticas de la tabla y del esquema, y vuelva a cargar NUM_ROWS y LAST_ANALYZED.
3. Cree un índice de árbol B y uno bitmap sobre columnas de prueba adecuadas. Vuelva a cargar, valide cada uno, reconstrúyalos y confirme la eliminación de cada índice de prueba. Compruebe que se rechazan los identificadores no válidos.
4. Compare la instancia, los parámetros, las sesiones y los conteos de sesiones con SQL Developer. Los cursores abiertos no deben ser negativos y deben verse en cada sesión.
5. En dos conexiones separadas de SQL Developer, actualice la misma fila de prueba sin confirmar para crear un par de sesión bloqueadora y sesión en espera. Actualice los bloqueos y los objetos bloqueados, verifique el SID y el serial, y confirme la terminación de la sesión bloqueadora desechable. Actualice de nuevo para comprobar que la espera termina. Revierta el trabajo de prueba restante y elimine los objetos de prueba.

Las pruebas automatizadas simulan JDBC y HTTP, y no sustituyen estas comprobaciones de integración con Oracle.

Verificación en la instancia local de Oracle 21c XE (28 de octubre de 2026): las siete vistas de monitoreo respondieron correctamente; los planes de ejecución devolvieron la jerarquía y el indicador de recorrido completo; las estadísticas de esquema y tabla reportaron 1000 filas de prueba y LAST_ANALYZED; los índices de árbol B y bitmap pasaron la creación, reconstrucción, validación y eliminación; una prueba de bloqueo con dos conexiones se detectó y se resolvió terminando la sesión bloqueadora de prueba exacta. El esquema temporal se eliminó. Las tres pantallas del frontend se comprobaron contra el backend en ejecución. Pruebas automatizadas en esa fecha: 314 pruebas del backend (2 omitidas, sin fallos) y 161 pruebas del frontend aprobadas; la compilación de producción del frontend y Maven verify fueron exitosos.

## Cómo contribuir

Consulte [CONTRIBUTING.md](CONTRIBUTING.md) para ver las convenciones de ramas, pull requests y código.

## Acerca del proyecto

Este proyecto se desarrolló como proyecto final del curso EIF-402 Administración de Bases de Datos de la Universidad Nacional, Costa Rica.
