# Copilot Instructions

Full standards in `CONVENTIONS.md` (project root). This file is a compact reference.

## Stack

Java 25, Spring Boot 4.x, Spring Data JPA, Spring Security 7, Thymeleaf, MySQL/H2.
Flyway is planned but not configured yet (schema managed by `ddl-auto`): do not create migrations.

## Architecture

Controller (thin, no logic) -> Service (all business logic, `@Transactional`) -> Repository (JPA interfaces).

## DTOs (mandatory for all controller I/O)

- Input: `CreateXRequest`, `UpdateXRequest` with `jakarta.validation` annotations
- Output: `XResponse`, `XSummaryResponse`
- Controllers use `@Valid` on all request DTOs
- Mapping: dedicated `XMapper` utility classes (non-instantiable, `public static` methods), called only
  from controllers — no MapStruct, no `BeanUtils.copyProperties()`
- Never expose JPA entities in controller signatures

## Entities

- Extend `AuditableEntity`. Implement `equals`/`hashCode` manually based on `id`
- Allowed Lombok: `@Getter`, `@Setter`, `@NoArgsConstructor`
- Forbidden: `@Data`, `@EqualsAndHashCode`, `@ToString`, `@Builder`

## Services

- `@Transactional(readOnly = true)` at class level, `@Transactional` on write methods
- Return `Optional<T>` for single lookups — never return `null`
- Never import from `dto.*`: services accept entities, primitives (≤ 3 params) or commands, and return
  entities
- Commands: `XCommand` in `<domain>/command/` for operations with > 3 params or spanning several
  entities — plain POJO, no validation or JPA annotations, built by the mapper in the controller
- Resource authorization: `@PreAuthorize("@authz.canXxx(authentication, #id)")` on service interface
  methods, backed by `shared/authorization/ResourceAuthorizationService` (`@Component("authz")`).
  Each check returns a `boolean`; `false` results in a 403

## Exceptions

- Domain-specific exceptions only — never `RuntimeException`, `IllegalArgumentException`, or JPA exceptions
- Single `@ControllerAdvice` handles all exceptions — controllers must not `try/catch`

## Naming and Packages

- Interfaces: `XService` (not `XServiceInterface`). Implementations: `XServiceImpl`
- Domain-first: each domain (`user/`, `pet/`, `clinic/`, `appointment/`, `medical/record/`,
  `medical/visit/`, `statistics/`) owns its layers as sub-packages: `entity/`, `repository/`,
  `service/`, `command/`, `dto/request/`, `dto/response/`, `mapper/`, `controller/`. Cross-cutting base
  classes live in `shared/`
- Service interface + implementation + validator co-located in `<domain>/service/`
- Repository base + query + statistics co-located in `<domain>/repository/`
- Role dashboards live in the `controller/` package of their domain (e.g.
  `user/controller/AdminDashboardController`)

## Logging

`@Slf4j` on services. SLF4J only. Never log passwords or personal data.

## Testing

Service logic with Mockito. Controllers with `@WebMvcTest`. Repositories with `@DataJpaTest`.

## Do NOT

- Add dependencies without being asked
- Put business logic in controllers
- Expose entities in controller signatures
- Catch exceptions in controllers
- Use `ddl-auto: update` outside test profile (temporary exception: `dev`, until Flyway is configured;
  never `prod`)
- Introduce new architectural patterns
- Return `null` from service methods
- Call `now()` without arguments — the current time always comes from the injected `Clock` (`LocalDate.now(clock)`)
