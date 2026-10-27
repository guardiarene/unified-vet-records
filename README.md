# PSyGCV — Unified Veterinary Medical Records

One medical record per pet, shared by every clinic on the platform.

[English](README.md) | [Español](README.es.md)

[![CI](https://github.com/guardiarene/unified-vet-records/actions/workflows/ci.yml/badge.svg)](https://github.com/guardiarene/unified-vet-records/actions/workflows/ci.yml)
[![CodeQL](https://github.com/guardiarene/unified-vet-records/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/guardiarene/unified-vet-records/actions/workflows/github-code-scanning/codeql)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-005F0F?logo=thymeleaf)

PSyGCV is a web application for veterinary clinics, built with Spring Boot and Thymeleaf. It
covers pets, appointments, clinic staff and clinical records, with role-based access.

---

## The problem

When a pet is treated at more than one veterinary clinic, its clinical history ends up scattered:
each clinic keeps its own notes and sees only part of the picture. Allergies, previous diseases,
treatments and vaccines recorded elsewhere are unknown to the veterinarian in front of the pet.

---

## Unified medical records

Each pet has exactly one medical record on the platform. Clinics do not keep separate copies:
every clinic that treats the pet adds its visits to the same record.

- **One record per pet.** The pet–record relationship is one-to-one, backed by a unique key, and
  the service rejects creating a second record for a pet that already has one.
- **Shared across clinics.** A veterinarian adds visits to a pet's record when the pet has an
  appointment at their clinic. Visits from different clinics accumulate in the same record,
  ordered by date, most recent first.
- **Visit content.** Each visit records the anamnesis, clinical exam, diagnoses, treatments and
  vaccines, together with the veterinarian who performed it.
- **Current anamnesis.** The record shows a consolidated anamnesis built from its visits: for each
  field (allergies, previous diseases, surgeries, current medication, diet, reproductive status
  and the last deworming, heat and birth dates) it takes the most recent value recorded.

```mermaid
flowchart LR
    P[Pet] --> R[Medical record]
    R --> A[Visits from Clinic A]
    R --> B[Visits from Clinic B]
    R --> C[Visits from Clinic C]
```

### Access: continuity of care versus privacy

Sharing a record across clinics trades the owner's privacy against clinical continuity. Access
requires an appointment at the clinic: records are not open to every clinic on the platform.

| Role         | Access                                                                                                                                                                                                                                                                                                      |
|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Veterinarian | Reads the full record, including visits from other clinics, when the pet has an appointment at the veterinarian's clinic. Any appointment counts, including a cancelled one. Edits the record's general observations under the same rule. Each veterinarian edits or removes only the visits they recorded. |
| Customer     | Read-only access to the records of their own pets.                                                                                                                                                                                                                                                          |
| Receptionist | No access to medical records or visits.                                                                                                                                                                                                                                                                     |

A clinic can currently create appointments without the owner's involvement, so an appointment
does not imply that the owner agreed to that clinic's access. Owner confirmation of
clinic-created appointments is on the [roadmap](#roadmap).

Unification happens among the clinics registered on the platform. Records are not exchanged with
external systems.

The access rules are implemented in
[`ResourceAuthorizationService`](src/main/java/tfg/psygcv/shared/authorization/ResourceAuthorizationService.java)
and the record logic in
[`MedicalRecordServiceImpl`](src/main/java/tfg/psygcv/medical/record/service/MedicalRecordServiceImpl.java).

---

## Features by role

After login, each user is redirected to the dashboard for their role.

| Role          | Features                                                                                                                                              |
|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| Customer      | Registration, pet management, clinic search, requesting and cancelling appointments, viewing their pets' medical records (read-only).               |
| Veterinarian  | Clinic registration, staff onboarding (veterinarians and receptionists), medical service catalog, medical records and visits, clinical reports.      |
| Receptionist  | Appointment scheduling and rescheduling, appointment status updates, appointment reports.                                                            |
| Administrator | User management.                                                                                                                                      |

---

## Engineering highlights

- **Resource-level authorization.** Service interfaces declare `@PreAuthorize` checks that
  delegate to
  [`ResourceAuthorizationService`](src/main/java/tfg/psygcv/shared/authorization/ResourceAuthorizationService.java)
  (for example,
  [`MedicalRecordService`](src/main/java/tfg/psygcv/medical/record/service/MedicalRecordService.java)).
  A resource that belongs to someone else is rejected with the same 403 as one that does not
  exist, so its existence stays hidden.
- **Domain invariants in the service layer.** Relationships that arrive as IDs from a form, such
  as a pet and its owner or a medical service and its clinic, are verified in the service, not
  only in the DTO (for example,
  [`AppointmentValidator`](src/main/java/tfg/psygcv/appointment/service/AppointmentValidator.java)).
- **Global error handling.**
  [`GlobalExceptionHandler`](src/main/java/tfg/psygcv/exception/GlobalExceptionHandler.java) maps
  domain exceptions to the correct HTTP status (400, 403, 404, 500) and logs unexpected errors
  with a correlation ID, without exposing stack traces.
- **Transparent soft delete.**
  [`AuditableEntity`](src/main/java/tfg/psygcv/shared/entity/AuditableEntity.java) carries
  `@SQLRestriction("active = true")`, so inactive rows are filtered out of every query.
- **DTO and mapper layer.** Controllers never expose JPA entities: request and response DTOs are
  converted by explicit, static mappers, and complex operations reach the service as command
  objects.
- **Per-environment profiles.** `dev`, `test` and `prod` isolate database and logging
  configuration; local credentials live outside the repository.
- **CI pipeline.** [`ci.yml`](.github/workflows/ci.yml) runs the build, Dependency Review and
  Conventional Commits validation on pull requests, with every Action pinned to a commit SHA.
  CodeQL runs through GitHub's default setup, and
  [Dependabot](.github/dependabot.yml) updates Maven dependencies and Actions with a cooldown
  period.
- **Conventions.** Project standards are documented in [`CONVENTIONS.md`](CONVENTIONS.md).

---

## Architecture

The code is organized by domain (vertical slices), and each domain owns its layers. Cross-cutting
infrastructure lives in `config/`, `shared/` and `exception/`.

```mermaid
flowchart TB
    subgraph domains["Domains: user · pet · clinic · appointment · medical (record, visit) · statistics"]
        direction LR
        C[controller] --> S[service]
        C --> M[mapper / dto]
        S --> V[validator]
        S --> R[repository]
        R --> E[entity]
    end
    CFG["config/<br/>security, audit"] -.-> domains
    SH["shared/<br/>authorization, base classes"] -.-> domains
    EX["exception/<br/>GlobalExceptionHandler"] -.-> domains
```

| Profile | Database               | Purpose                                                                         |
|---------|------------------------|---------------------------------------------------------------------------------|
| `dev`   | MySQL                  | Local development. Imports credentials from `application-local.yaml`            |
| `test`  | MySQL (Testcontainers) | Test runs and CI, in a MySQL 8.4 container started by the tests                 |
| `prod`  | MySQL                  | Production. Credentials from environment variables                              |
| `debug` | —                      | Optional, local only. Logs SQL statements and bound parameters; use `dev,debug` |

In every profile, Flyway creates and migrates the schema from
[`db/migration`](src/main/resources/db/migration), and Hibernate only validates it.

---

## Getting started

### Requirements

- JDK 25
- MySQL 8.4
- Docker, to run the tests (they start a MySQL container with Testcontainers) and the Docker Compose
  stack

Maven is not required: the project includes the Maven Wrapper.

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/guardiarene/unified-vet-records.git
   cd unified-vet-records
   ```

2. **Create the database** (empty: Flyway creates the tables when the application starts):
   ```sql
   CREATE DATABASE psygcv_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
   ```
   The charset and collation are set when the database is created: the migrations do not declare
   them.

   A database created by an earlier version of the project (when Hibernate generated the schema
   with `ddl-auto`) must be dropped and recreated empty: Flyway does not accept a non-empty schema
   without a migration history.

3. **Configure the credentials** in an `application-local.yaml` file at the project root (not
   versioned; imported by the `dev` profile):
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/psygcv_dev
       username: your_user
       password: your_password
   ```
   Alternatively, set the `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` environment variables. If
   `application-local.yaml` exists, its values take precedence.

4. **Build and verify:**
   ```bash
   ./mvnw clean verify
   ```

5. **Run the application** with the `dev` profile:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```
   To log SQL statements and their parameter values, add the optional `debug` profile (local use
   only):
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,debug
   ```

The application is available at [http://localhost:8080](http://localhost:8080).

**Windows:** use `.\mvnw.cmd` instead of `./mvnw`. In PowerShell, quote every `-D` argument,
because PowerShell splits unquoted arguments at the first dot:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### Initial administrator

An empty database has no administrator, and the user management pages require one to exist. At
startup the application creates the initial administrator from five environment variables, all
required together:

| Variable           | Value                                     |
|--------------------|-------------------------------------------|
| `ADMIN_EMAIL`      | Email of the administrator account        |
| `ADMIN_PASSWORD`   | Password, at least 8 characters           |
| `ADMIN_FIRST_NAME` | First name                                |
| `ADMIN_LAST_NAME`  | Last name                                 |
| `ADMIN_PHONE`      | Phone number                              |

```bash
export ADMIN_EMAIL=admin@example.com
export ADMIN_PASSWORD=change-me-123
export ADMIN_FIRST_NAME=Ada
export ADMIN_LAST_NAME=Admin
export ADMIN_PHONE=5551234567
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

PowerShell equivalent:

```powershell
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "change-me-123"
$env:ADMIN_FIRST_NAME = "Ada"
$env:ADMIN_LAST_NAME = "Admin"
$env:ADMIN_PHONE = "5551234567"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Behavior:

- **No variable defined** (the normal case in tests, CI and development): the bootstrap does
  nothing.
- **All five defined**: if no active administrator exists, one is created with the
  `SYSTEM_ADMINISTRATOR` role. The log records only the user ID, never the email or the password.
- **Some defined, others missing**: the application does not start, and the error names the missing
  variables (never their values).
- **An active administrator already exists**: nothing is created, so restarting with the same
  variables is safe.

Security recommendations once the administrator exists:

1. Change the administrator's password from the user management pages.
2. Remove `ADMIN_PASSWORD` from the environment.

### Run with Docker Compose

`docker-compose.yml` runs the application with the `prod` profile and a MySQL 8.4 container. Both
images are pinned by tag and digest; the database uses a named volume and is not published to the
host. Ports are published only on `127.0.0.1`, both services have a memory limit and a healthcheck,
and the application container runs as a non-root user with `no-new-privileges` and all capabilities
dropped.

1. **Create the environment file** from the versioned example:
   ```bash
   cp .env.example .env
   ```
   ```powershell
   Copy-Item .env.example .env
   ```
   `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` and `MYSQL_ROOT_PASSWORD` are **required**: if
   any is missing or empty, Compose stops with an error that names it and points to `.env`. The five
   `ADMIN_*` variables are **optional**: leave them empty to start without an initial administrator,
   or fill all five to create it (see [Initial administrator](#initial-administrator)).

2. **Start the stack:**
   ```bash
   docker compose up --build
   ```
   MySQL becomes healthy first; then the application applies the Flyway migrations and becomes
   healthy.

3. The application is available at [http://localhost:8080](http://localhost:8080) and its health
   endpoint at [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health). The
   health endpoint needs no authentication and does not show details.

4. **Stop the stack:**
   ```bash
   docker compose down
   ```
   `docker compose down -v` also removes the named volume: **it deletes all data**.

The profile can be changed with `SPRING_PROFILES_ACTIVE` in `.env`.

---

## Roadmap

**Next**

- Owner confirmation for appointments created by a clinic, before they grant access to the
  medical record
- Broader automated test coverage (service, repository and web layers)
- A continuous delivery pipeline
- Cloud deployment with containers

**From the original design**

- Account lockout after failed login attempts
- A complete password policy
- Password expiration
- Multi-factor authentication

**Long term**

- A React frontend consuming a REST API

---

The project started as a final degree project and continues to evolve as a portfolio project,
applying production practices.
