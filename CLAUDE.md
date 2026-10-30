# CLAUDE.md

Read `CONVENTIONS.md` before starting any task. It is the single source of truth for all project standards.

## Commands

```bash
./mvnw clean verify                                    # Full build (checkstyle + tests) — run before completing any task
./mvnw test                                            # Tests only
./mvnw test -Dtest=ClassName                           # Single test class
./mvnw checkstyle:check                                # Style check only
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # Local dev server
```

On Windows (PowerShell), use `.\mvnw.cmd` instead of `./mvnw` and quote every `-D` argument: PowerShell
splits unquoted arguments at the first dot (`.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`).

## Workflow

### Before Writing Code

1. Read `CONVENTIONS.md` if you haven't this session
2. Read existing files in the affected domain (entity, repository, service, DTO, mapper, controller)
3. Identify established patterns — naming, structure, error handling
4. Follow those patterns exactly — do not introduce new conventions

### After Writing Code

1. `./mvnw checkstyle:check` — fix all violations
2. `./mvnw test` — all tests must pass
3. `./mvnw clean verify` — must pass before marking any task complete

## Common Tasks

### Implementing a Feature

1. Identify affected layers: entity -> repository -> service -> DTO/mapper -> controller
2. Read existing code in the same domain to match patterns
3. Implement bottom-up: entity -> repository -> service -> DTO/mapper -> controller
4. Write tests for each layer (service tests mandatory, controller tests for new endpoints)
5. Run `./mvnw clean verify`

### Adding an Entity

1. Read an existing entity — replicate the `equals`/`hashCode` pattern
2. Create entity in `<domain>/entity/` with JPA annotations
3. Create repository in `<domain>/repository/`
4. Create DTOs: `CreateXRequest`, `UpdateXRequest` in `<domain>/dto/request/`; `XResponse` (and
   `XSummaryResponse` if needed) in `<domain>/dto/response/`
5. Create mapper in `<domain>/mapper/`
6. Create command objects in `<domain>/command/` if needed
7. Create service interface and implementation in `<domain>/service/`
8. Create controller in `<domain>/controller/` using DTOs with `@Valid`
9. Add domain-specific exceptions in `exception/` if needed
10. Add a new Flyway migration in `src/main/resources/db/migration/` (`V{n}__description.sql`);
    `SchemaDriftTest` verifies that it matches the entity
11. Write tests — run `./mvnw clean verify`

### Fixing a Bug

1. Read the related code first
2. Write a failing test that reproduces the bug (when possible)
3. Implement the fix
4. Verify the test passes
5. Run `./mvnw clean verify`

## Critical Rules

- **Never** modify code without reading existing files in the same domain first
- **Never** add dependencies to `pom.xml` unless explicitly asked
- **Never** expose JPA entities through controllers — always use DTOs
- **Never** put business logic in controllers
- **Never** catch exceptions in controllers — `@ControllerAdvice` handles them
- **Never** modify `application-prod.yaml` unless explicitly instructed
- **Never** use `BeanUtils.copyProperties()` — map fields explicitly
- **Never** modify a Flyway migration that has already been applied — every schema change is a new
  migration `V{n}__description.sql`
- **Never** set `ddl-auto` to anything other than `validate`, in any profile
- **Always** run `./mvnw clean verify` before considering a task complete
- **Always** follow existing patterns over inventing new ones

## When Uncertain

If a pattern is ambiguous or inconsistent in the codebase, ask before proceeding.
Do not guess. Do not silently pick one pattern over another.
