# PSyGCV — Historias clínicas veterinarias unificadas

Una historia clínica por mascota, compartida por todas las clínicas de la plataforma.

[English](README.md) | [Español](README.es.md)

[![CI](https://github.com/guardiarene/unified-vet-records/actions/workflows/ci.yml/badge.svg)](https://github.com/guardiarene/unified-vet-records/actions/workflows/ci.yml)
[![CodeQL](https://github.com/guardiarene/unified-vet-records/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/guardiarene/unified-vet-records/actions/workflows/github-code-scanning/codeql)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?logo=thymeleaf)

PSyGCV es una aplicación web para clínicas veterinarias, desarrollada con Spring Boot y Thymeleaf.
Abarca mascotas, turnos, personal de las clínicas e historias clínicas, con acceso diferenciado
por rol.

---

## El problema

Cuando una mascota se atiende en más de una clínica veterinaria, su historia clínica queda
repartida: cada clínica guarda sus propias notas y ve solo una parte. Las alergias, enfermedades
previas, tratamientos y vacunas registrados en otro lugar son desconocidos para el veterinario que
tiene a la mascota delante.

---

## Historias clínicas unificadas

Cada mascota tiene exactamente una historia clínica en la plataforma. Las clínicas no guardan
copias separadas: cada clínica que atiende a la mascota agrega sus visitas a la misma historia.

- **Una historia por mascota.** La relación entre mascota e historia es uno a uno, respaldada por
  una clave única, y el servicio rechaza crear una segunda historia para una mascota que ya tiene
  una.
- **Compartida entre clínicas.** Un veterinario agrega visitas a la historia de una mascota cuando
  la mascota tiene un turno en su clínica. Las visitas de distintas clínicas se acumulan en la
  misma historia, ordenadas por fecha, de la más reciente a la más antigua.
- **Contenido de cada visita.** Cada visita registra la anamnesis, el examen clínico, los
  diagnósticos, los tratamientos y las vacunas, junto con el veterinario que la realizó.
- **Anamnesis vigente.** La historia muestra una anamnesis consolidada a partir de sus visitas:
  para cada campo (alergias, enfermedades previas, cirugías, medicación actual, dieta, estado
  reproductivo y las fechas de la última desparasitación, el último celo y el último parto) toma
  el valor registrado más reciente.

```mermaid
flowchart LR
    P[Mascota] --> R[Historia clínica]
    R --> A[Visitas de la clínica A]
    R --> B[Visitas de la clínica B]
    R --> C[Visitas de la clínica C]
```

### Acceso: continuidad clínica frente a privacidad

Compartir una historia entre clínicas implica equilibrar la privacidad del dueño con la
continuidad clínica. El acceso requiere un turno en la clínica: las historias no están abiertas a
todas las clínicas de la plataforma.

| Rol           | Acceso                                                                                                                                                                                                                                                                                                                            |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Veterinario   | Consulta la historia completa, incluidas las visitas de otras clínicas, cuando la mascota tiene un turno en la clínica del veterinario. Cuenta cualquier turno, incluso uno cancelado. Edita las observaciones generales de la historia con la misma regla. Cada veterinario modifica o da de baja solo las visitas que registró. |
| Cliente       | Acceso de solo lectura a las historias de sus propias mascotas.                                                                                                                                                                                                                                                                   |
| Recepcionista | Sin acceso a historias clínicas ni a visitas.                                                                                                                                                                                                                                                                                     |

Hoy una clínica puede crear turnos sin intervención del dueño, así que un turno no implica que el
dueño haya aceptado el acceso de esa clínica. La confirmación del dueño para los turnos creados
por una clínica está en el [roadmap](#roadmap).

La unificación ocurre entre las clínicas registradas en la plataforma. Las historias no se
intercambian con sistemas externos.

Las reglas de acceso están implementadas en
[`ResourceAuthorizationService`](src/main/java/tfg/psygcv/shared/authorization/ResourceAuthorizationService.java)
y la lógica de las historias en
[`MedicalRecordServiceImpl`](src/main/java/tfg/psygcv/medical/record/service/MedicalRecordServiceImpl.java).

---

## Funcionalidades por rol

Tras iniciar sesión, cada usuario es redirigido al dashboard de su rol.

| Rol           | Funcionalidades                                                                                                                                         |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| Cliente       | Registro, gestión de mascotas, búsqueda de clínicas, solicitud y cancelación de turnos, consulta de las historias clínicas de sus mascotas (solo lectura). |
| Veterinario   | Registro de la clínica, alta de personal (veterinarios y recepcionistas), catálogo de servicios médicos, historias clínicas y visitas, reportes clínicos. |
| Recepcionista | Agenda y reprogramación de turnos, actualización del estado de los turnos, reportes de turnos.                                                         |
| Administrador | Gestión de usuarios.                                                                                                                                    |

---

## Aspectos técnicos destacados

- **Autorización a nivel de recurso.** Las interfaces de servicio declaran comprobaciones
  `@PreAuthorize` que delegan en
  [`ResourceAuthorizationService`](src/main/java/tfg/psygcv/shared/authorization/ResourceAuthorizationService.java)
  (por ejemplo,
  [`MedicalRecordService`](src/main/java/tfg/psygcv/medical/record/service/MedicalRecordService.java)).
  Un recurso que pertenece a otro usuario se rechaza con el mismo 403 que uno que no existe, de
  modo que su existencia queda oculta.
- **Invariantes de dominio en la capa de servicio.** Las relaciones que llegan como IDs desde un
  formulario, como una mascota y su dueño o un servicio médico y su clínica, se verifican en el
  servicio, no solo en el DTO (por ejemplo,
  [`AppointmentValidator`](src/main/java/tfg/psygcv/appointment/service/AppointmentValidator.java)).
- **Manejo global de errores.**
  [`GlobalExceptionHandler`](src/main/java/tfg/psygcv/exception/GlobalExceptionHandler.java)
  traduce las excepciones de dominio al status HTTP correcto (400, 403, 404, 500) y registra los
  errores inesperados con un ID de correlación, sin exponer stack traces.
- **Soft delete transparente.**
  [`AuditableEntity`](src/main/java/tfg/psygcv/shared/entity/AuditableEntity.java) lleva
  `@SQLRestriction("active = true")`, de modo que las filas inactivas quedan fuera de todas las
  consultas.
- **Capa de DTOs y mappers.** Los controllers nunca exponen entidades JPA: los DTOs de request y
  de response se convierten con mappers estáticos y explícitos, y las operaciones complejas llegan
  al servicio como objetos command.
- **Perfiles por entorno.** `dev`, `test` y `prod` aíslan la configuración de base de datos y de
  logging; las credenciales locales quedan fuera del repositorio.
- **Pipeline de CI.** [`ci.yml`](.github/workflows/ci.yml) ejecuta el build, Dependency Review y
  la validación de Conventional Commits en los pull requests, con cada Action fijada a un SHA de
  commit. CodeQL corre mediante la configuración por defecto de GitHub, y
  [Dependabot](.github/dependabot.yml) actualiza las dependencias de Maven y las Actions con un
  período de cooldown.
- **Convenciones.** Los estándares del proyecto están documentados en
  [`CONVENTIONS.md`](CONVENTIONS.md).

---

## Arquitectura

El código se organiza por dominio (vertical slices), y cada dominio contiene sus propias capas. La
infraestructura transversal vive en `config/`, `shared/` y `exception/`.

```mermaid
flowchart TB
    subgraph domains["Dominios: user · pet · clinic · appointment · medical (record, visit) · statistics"]
        direction LR
        C[controller] --> S[service]
        C --> M[mapper / dto]
        S --> V[validator]
        S --> R[repository]
        R --> E[entity]
    end
    CFG["config/<br/>security, audit"] -.-> domains
    SH["shared/<br/>authorization, clases base"] -.-> domains
    EX["exception/<br/>GlobalExceptionHandler"] -.-> domains
```

| Perfil  | Base de datos          | Propósito                                                                                     |
|---------|------------------------|-----------------------------------------------------------------------------------------------|
| `dev`   | MySQL                  | Desarrollo local. Importa las credenciales de `application-local.yaml`                        |
| `test`  | MySQL (Testcontainers) | Tests y CI, en un contenedor de MySQL 8.4 que levantan los propios tests                      |
| `prod`  | MySQL                  | Producción. Credenciales desde variables de entorno                                           |
| `debug` | —                      | Opcional, solo para uso local. Registra las sentencias SQL y sus parámetros; usar `dev,debug` |

En todos los perfiles, Flyway crea y migra el esquema a partir de
[`db/migration`](src/main/resources/db/migration), y Hibernate solo lo valida.

---

## Primeros pasos

### Requisitos

- JDK 25
- MySQL 8.4
- Docker, para correr los tests (levantan un contenedor MySQL con Testcontainers) y el stack de
  Docker Compose

No hace falta instalar Maven: el proyecto incluye el Maven Wrapper.

### Pasos

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/guardiarene/unified-vet-records.git
   cd unified-vet-records
   ```

2. **Crear la base de datos** (vacía: Flyway crea las tablas al arrancar la aplicación):
   ```sql
   CREATE DATABASE psygcv_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
   El charset y la colación se fijan al crear la base: las migraciones no los declaran.

   Una base creada con una versión anterior del proyecto (cuando Hibernate generaba el esquema con
   `ddl-auto`) debe eliminarse y recrearse vacía: Flyway no acepta un esquema no vacío sin
   historial de migraciones.

3. **Configurar las credenciales** en un archivo `application-local.yaml` en la raíz del proyecto
   (no versionado; lo importa el perfil `dev`):
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/psygcv_dev
       username: tu_usuario
       password: tu_contraseña
   ```
   Como alternativa, definir las variables de entorno `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. Si
   existe `application-local.yaml`, sus valores tienen prioridad.

4. **Compilar y verificar:**
   ```bash
   ./mvnw clean verify
   ```

5. **Ejecutar la aplicación** con el perfil `dev`:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   Para registrar las sentencias SQL y los valores de sus parámetros, sumar el perfil opcional
   `debug` (solo para uso local):
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,debug
   ```

La aplicación queda disponible en [http://localhost:8080](http://localhost:8080).

**Windows:** usar `.\mvnw.cmd` en lugar de `./mvnw`. En PowerShell, poner entre comillas cada
argumento `-D`, porque PowerShell divide los argumentos sin comillas en el primer punto:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### Administrador inicial

Una base vacía no tiene administrador, y las páginas de gestión de usuarios exigen que exista uno.
Al arrancar, la aplicación crea el administrador inicial a partir de cinco variables de entorno,
todas obligatorias en conjunto:

| Variable           | Valor                                  |
|--------------------|----------------------------------------|
| `ADMIN_EMAIL`      | Email de la cuenta del administrador   |
| `ADMIN_PASSWORD`   | Contraseña, al menos 8 caracteres      |
| `ADMIN_FIRST_NAME` | Nombre                                 |
| `ADMIN_LAST_NAME`  | Apellido                               |
| `ADMIN_PHONE`      | Teléfono                               |

```bash
export ADMIN_EMAIL=admin@example.com
export ADMIN_PASSWORD=change-me-123
export ADMIN_FIRST_NAME=Ada
export ADMIN_LAST_NAME=Admin
export ADMIN_PHONE=5551234567
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Equivalente en PowerShell:

```powershell
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "change-me-123"
$env:ADMIN_FIRST_NAME = "Ada"
$env:ADMIN_LAST_NAME = "Admin"
$env:ADMIN_PHONE = "5551234567"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Comportamiento:

- **Ninguna variable definida** (el caso normal en tests, CI y desarrollo): el bootstrap no hace
  nada.
- **Las cinco definidas**: si no existe un administrador activo, se crea uno con el rol
  `SYSTEM_ADMINISTRATOR`. El log registra solo el ID del usuario, nunca el email ni la contraseña.
- **Algunas definidas y otras no**: la aplicación no arranca, y el error nombra las variables
  faltantes (nunca sus valores).
- **Ya existe un administrador activo**: no se crea nada, así que reiniciar con las mismas variables
  es seguro.

Recomendaciones de seguridad una vez creado el administrador:

1. Cambiar la contraseña del administrador desde las páginas de gestión de usuarios.
2. Quitar `ADMIN_PASSWORD` del entorno.

### Ejecutar con Docker Compose

`docker-compose.yml` levanta la aplicación con el perfil `prod` y un contenedor MySQL 8.4. Ambas
imágenes están fijadas por tag y digest; la base usa un volumen nombrado y no se publica al host.
Los puertos se publican solo en `127.0.0.1`, los dos servicios tienen límite de memoria y healthcheck,
y el contenedor de la aplicación corre con un usuario no root, `no-new-privileges` y todas las
capacidades quitadas.

1. **Crear el archivo de entorno** a partir del ejemplo versionado:
   ```bash
   cp .env.example .env
   ```
   ```powershell
   Copy-Item .env.example .env
   ```
   `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` y `MYSQL_ROOT_PASSWORD` son **obligatorias**: si
   falta alguna o está vacía, Compose se detiene con un error que la nombra e indica `.env`. Las cinco
   variables `ADMIN_*` son **opcionales**: dejarlas vacías para arrancar sin administrador inicial, o
   completar las cinco para crearlo (ver [Administrador inicial](#administrador-inicial)).

2. **Levantar el stack:**
   ```bash
   docker compose up --build
   ```
   MySQL queda healthy primero; después la aplicación aplica las migraciones de Flyway y queda
   healthy.

3. La aplicación queda disponible en [http://localhost:8080](http://localhost:8080) y su endpoint de
   salud en [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health). El
   endpoint de salud no pide autenticación y no muestra detalles.

4. **Bajar el stack:**
   ```bash
   docker compose down
   ```
   `docker compose down -v` también elimina el volumen nombrado: **borra todos los datos**.

El perfil se puede cambiar con `SPRING_PROFILES_ACTIVE` en `.env`.

---

## Roadmap

**Próximos pasos**

- La confirmación del dueño para los turnos creados por una clínica, antes de que den acceso a la
  historia clínica
- Más cobertura de tests automatizados (servicios, repositorios y web)
- Un pipeline de entrega continua
- Despliegue en la nube con contenedores

**Del diseño original**

- Bloqueo de cuentas tras intentos fallidos de inicio de sesión
- Una política de contraseñas completa
- Expiración de contraseñas
- Autenticación multifactor

**Largo plazo**

- Un frontend en React que consuma una API REST

---

El proyecto comenzó como Trabajo Final de Grado y sigue evolucionando como proyecto de portfolio,
aplicando prácticas de producción.
