# Project Conventions

Single source of truth for all development standards in this project.
Referenced by `CLAUDE.md` and `.github/copilot-instructions.md`.

---

## Stack

- Java 25, Spring Boot 4.x, Maven (via the Maven Wrapper)
- Spring Data JPA, Spring Security 7, Thymeleaf
- MySQL 8.4 in every profile (tests run it with Testcontainers)
- Flyway migrations manage the schema in every profile; Hibernate only validates it
- Lombok (restricted subset), Jakarta Validation
- JUnit 6 (Jupiter), Mockito, AssertJ
- CI: GitHub Actions (`./mvnw -B -ntp clean verify`)

---

## Architecture

Domain-first (vertical slice) with layered sub-packages: **Domain → Controller / Service / Repository**

Each domain owns all its layers. Cross-cutting infrastructure lives in `shared/`.

| Layer      | Responsibility                             | Rules                                                                                                                                                           |
|------------|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Controller | Receive HTTP requests, delegate, respond   | No business logic. No `try/catch`. No entity types in signatures. Always use DTOs. Calls mappers to convert between DTOs and entities/commands.                 |
| Service    | All business logic                         | `@Transactional`. Returns `Optional<T>` for single lookups. Never returns `null`. Never imports from `dto.*`. Accepts entities, primitives, or command objects. |
| Repository | Data access via JPA interfaces             | No business logic. No transactions. No service/controller imports.                                                                                              |
| Entity     | Persistence model                          | No business logic. No presentation logic. No service/controller imports.                                                                                        |
| DTO        | Transfer objects for controller I/O        | Jakarta Validation annotations. No JPA annotations. No entity imports. Input DTOs in `<domain>/dto/request/`, output DTOs in `<domain>/dto/response/`.          |
| Mapper     | DTO/Entity conversion and command building | Non-instantiable utility class. All methods `public static`. Called only from controllers. No framework dependencies.                                           |
| Command    | Input contract for complex service ops     | Plain POJO in `<domain>/command/`. No validation annotations. No JPA annotations. Used when parameters exceed 3 or map to no single entity.                     |
| Exception  | Domain-specific error types                | Extend `RuntimeException`. Handled exclusively by `@ControllerAdvice`.                                                                                          |
| Validator  | Complex business validation                | Called by services. Throws domain exceptions.                                                                                                                   |

---

## Package Structure

```
tfg.psygcv
├── config/                        # Configuration (cross-cutting infra)
│   ├── audit/                     #   JpaAuditConfig
│   ├── bootstrap/                 #   InitialAdminBootstrapRunner, AdminBootstrapProperties
│   ├── security/                  #   SecurityConfig, AuthenticatedUser, SuccessHandler
│   ├── time/                      #   ClockConfig
│   └── util/                      #   JsonUtil and other infrastructure utilities
│
├── shared/                        # Cross-cutting base classes
│   ├── authorization/             #   ResourceAuthorizationService (@Component("authz"))
│   ├── constant/                  #   RouteConstant
│   ├── controller/                #   BaseController, PublicController
│   ├── entity/                    #   AuditableEntity
│   └── validation/                #   BaseValidator
│
├── user/                          # User domain
│   ├── command/                   #   RegisterCustomerCommand, RegisterVeterinarianCommand,
│   │                              #   CreateStaffCommand, CreateAdminUserCommand,
│   │                              #   UpdateAdminUserCommand, UpdateUserProfileCommand
│   ├── controller/                #   UserController, ProfileController, AdminDashboardController
│   ├── dto/
│   │   ├── request/               #   CreateUserRequest, CreateStaffRequest, CreateAdminUserRequest,
│   │   │                          #   UpdateUserRequest, UpdateAdminUserRequest
│   │   └── response/              #   UserResponse, UserSummaryResponse
│   ├── entity/                    #   User, Role
│   ├── mapper/                    #   UserMapper
│   ├── repository/                #   UserRepository
│   └── service/                   #   UserService, UserServiceImpl, UserValidator
│
├── pet/                           # Pet domain
│   ├── command/                   #   CreatePetCommand, UpdatePetCommand
│   ├── controller/                #   PetController
│   ├── dto/
│   │   ├── request/               #   CreatePetRequest, UpdatePetRequest
│   │   └── response/              #   PetResponse, PetSummaryResponse
│   ├── entity/                    #   Pet, Species, Sex, Breed, Temperament, ReproductiveStatus
│   ├── mapper/                    #   PetMapper
│   ├── repository/                #   PetRepository
│   └── service/                   #   PetService, PetServiceImpl, PetValidator
│
├── clinic/                        # Clinic domain
│   ├── command/                   #   RegisterClinicWithVeterinarianCommand, UpdateClinicCommand,
│   │                              #   CreateMedicalServiceCommand, UpdateMedicalServiceCommand
│   ├── controller/                #   VeterinaryClinicController, MedicalServiceController,
│   │                              #   ClinicManagementController, StaffController,
│   │                              #   CustomerDashboardController
│   ├── dto/
│   │   ├── request/               #   RegisterClinicRequest, UpdateClinicRequest,
│   │   │                          #   CreateMedicalServiceRequest, UpdateMedicalServiceRequest
│   │   └── response/              #   VeterinaryClinicResponse, VeterinaryClinicSummaryResponse,
│   │                              #   MedicalServiceResponse
│   ├── entity/                    #   VeterinaryClinic, MedicalService
│   ├── mapper/                    #   VeterinaryClinicMapper, MedicalServiceMapper
│   ├── repository/                #   VeterinaryClinicRepository, MedicalServiceRepository
│   └── service/                   #   VeterinaryClinicService, VeterinaryClinicServiceImpl,
│                                  #   VeterinaryClinicValidator, MedicalServiceService,
│                                  #   MedicalServiceServiceImpl, MedicalServiceValidator
│
├── appointment/                   # Appointment domain
│   ├── command/                   #   ScheduleAppointmentCommand, RescheduleAppointmentCommand
│   ├── controller/                #   CustomerAppointmentController,
│   │                              #   ReceptionistAppointmentController,
│   │                              #   ReceptionistDashboardController
│   ├── dto/
│   │   ├── request/               #   CreateClientAppointmentRequest, ScheduleAppointmentRequest,
│   │   │                          #   RescheduleAppointmentRequest
│   │   └── response/              #   AppointmentResponse, AppointmentSummaryResponse
│   ├── entity/                    #   Appointment, AppointmentStatus
│   ├── mapper/                    #   AppointmentMapper
│   ├── repository/                #   AppointmentRepository, AppointmentQueryRepository,
│   │                              #   AppointmentStatisticsRepository
│   └── service/                   #   AppointmentService, AppointmentServiceImpl,
│                                  #   AppointmentValidator
│
├── medical/                       # Medical domain (two sub-domains)
│   ├── record/                    #   Medical record sub-domain
│   │   ├── command/               #     CreateMedicalRecordCommand, UpdateMedicalRecordCommand
│   │   ├── controller/            #     MedicalRecordController, VeterinarianDashboardController
│   │   ├── dto/
│   │   │   ├── request/           #     CreateMedicalRecordRequest, UpdateMedicalRecordRequest
│   │   │   └── response/          #     MedicalRecordResponse, MedicalRecordSummaryResponse
│   │   ├── entity/                #     MedicalRecord
│   │   ├── mapper/                #     MedicalRecordMapper
│   │   ├── repository/            #     MedicalRecordRepository, MedicalRecordQueryRepository
│   │   └── service/               #     MedicalRecordService, MedicalRecordServiceImpl,
│   │                              #     MedicalRecordValidator
│   └── visit/                     #   Visit sub-domain (Visit + child entities)
│       ├── command/               #     CreateVisitCommand, UpdateVisitCommand
│       ├── controller/            #     VisitController
│       ├── dto/
│       │   ├── request/           #     CreateVisitRequest, UpdateVisitRequest,
│       │   │                      #     AnamnesisRequest, ClinicalExamRequest,
│       │   │                      #     DiagnosticRequest, TreatmentRequest, VaccineRequest
│       │   └── response/          #     VisitResponse, AnamnesisResponse, ClinicalExamResponse,
│       │                          #     DiagnosticResponse, TreatmentResponse, VaccineResponse
│       ├── entity/                #     Visit, Anamnesis, ClinicalExam, Diagnostic,
│       │                          #     Treatment, Vaccine, VisitType
│       ├── mapper/                #     VisitMapper, AnamnesisMapper, ClinicalExamMapper,
│       │                          #     DiagnosticMapper, TreatmentMapper, VaccineMapper
│       ├── repository/            #     VisitRepository, AnamnesisRepository,
│       │                          #     ClinicalExamRepository, DiagnosticRepository,
│       │                          #     DiagnosticStatisticsRepository, TreatmentRepository,
│       │                          #     TreatmentStatisticsRepository, VaccineRepository
│       └── service/               #     VisitService, VisitServiceImpl, VisitValidator
│
├── statistics/                    # Statistics / reporting domain
│   ├── controller/                #   ReportController
│   └── service/                   #   StatisticsService, StatisticsServiceImpl,
│                                  #   StatisticsValidator
│
└── exception/                     # Global exception handling
    ├── GlobalExceptionHandler     #   @ControllerAdvice for all exceptions
    └── domain/                    #   BusinessRuleException, DuplicateEmailException,
                                   #   ResourceNotFoundException, UnauthorizedClinicAccessException
```

### Principles

- **Domain-first organization**: each top-level package represents a bounded domain (`user/`, `pet/`,
  `clinic/`, `appointment/`, `medical/`, `statistics/`), containing all its layers as sub-packages
- Each domain has a consistent internal structure: `entity/`, `repository/`, `service/`, `command/`,
  `dto/`, `mapper/`, `controller/`
- The `medical/` domain is split into two sub-domains (`record/`, `visit/`) because MedicalRecord and
  Visit are separate aggregate roots with distinct lifecycles
- Cross-cutting base classes live in `shared/` (`AuditableEntity`, `BaseController`, `BaseValidator`,
  `RouteConstant`)
- Domain enums are co-located with their entities in `<domain>/entity/` — no separate `enums/` package
- DTOs are split by direction inside `<domain>/dto/`: input DTOs in `dto/request/`, output DTOs in
  `dto/response/` — this makes the data flow explicit and keeps large domains navigable
- Command objects live in `<domain>/command/`, separate from services
- Controllers follow **single responsibility**: one controller per cohesive set of endpoints (e.g.,
  `CustomerAppointmentController` vs `ReceptionistAppointmentController`)
- Max nesting: 5 levels (for `medical/record/dto/request/` and `medical/visit/dto/response/`)
- No catch-all packages: `utils/`, `helpers/`, `misc/`, `common/`, `base/`

### Design Principles (SOLID)

The domain-first architecture is guided by the SOLID principles. The table below maps each principle to where
it materializes in the codebase:

| Principle                 | Application in the project                                                                                                                                                                                                                              |
|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Single Responsibility** | Each layer has one reason to change: controllers handle HTTP, services hold business logic, repositories manage persistence, mappers convert between types, validators enforce rules.                                                                   |
| **Open / Closed**         | Service interfaces (`XService`) define the contract; implementations (`XServiceImpl`) can be swapped or decorated without modifying callers. New domain exceptions extend `RuntimeException` without changing `GlobalExceptionHandler` logic.           |
| **Liskov Substitution**   | `AuditableEntity` subclasses are usable wherever `AuditableEntity` is expected. `BaseValidator` subclasses honour the base contract. When overriding, never weaken preconditions or strengthen postconditions.                                          |
| **Interface Segregation** | Repositories are split by intent (`AppointmentRepository`, `AppointmentQueryRepository`, `AppointmentStatisticsRepository`) so consumers depend only on the queries they need. Service interfaces expose only the operations relevant to their callers. |
| **Dependency Inversion**  | Controllers depend on service interfaces, not implementations. Services never import from `dto.*` — the controller adapts between the presentation layer and the domain. Command objects decouple service input from presentation DTOs.                 |

When adding new code, verify that the change does not violate any of these principles. If a class starts
accumulating multiple reasons to change, split it.

---

## Code Style

- Google Java Style, checked by Checkstyle with `config/checkstyle/google_checks.xml` (configured in
  `pom.xml`)
- Max line length: 100 characters (`LineLength` rule)
- Checkstyle runs in the `verify` phase (`./mvnw clean verify`, locally and in CI); `./mvnw test` does
  not run it
- Severity: `google_checks.xml` sets `severity="warning"` globally and no rule overrides it, so every
  rule currently reports a warning and none fails the build. The plugin fails the build only on
  `error`-severity violations, or if Checkstyle itself cannot run
- Readability over cleverness
- No suppressed warnings without a comment explaining why

### Javadoc

- **Mandatory** on all public methods of service interfaces (`XService`) — describe what the method does,
  its parameters, return value, and exceptions thrown
- **Optional** on service implementations, mappers, validators, and controllers — add only when the logic
  is non-obvious
- Do not add Javadoc to getters, setters, or trivially clear methods

---

## Naming Conventions

| Element                | Convention                         | Example                                               |
|------------------------|------------------------------------|-------------------------------------------------------|
| Classes                | `PascalCase`                       | `AppointmentService`                                  |
| Methods                | `camelCase`                        | `findByClinicId`                                      |
| Constants              | `UPPER_SNAKE_CASE`                 | `MAX_RETRY_COUNT`                                     |
| URL paths              | `kebab-case`                       | `/medical-records/{id}`                               |
| Boolean methods        | `is`, `has`, `can`                 | `isActive()`, `hasAppointments()`                     |
| Service interfaces     | `XService`                         | `UserService`, `PetService`                           |
| Service impls          | `XServiceImpl`                     | `UserServiceImpl`, `PetServiceImpl`                   |
| DTOs (input, CRUD)     | `CreateXRequest`, `UpdateXRequest` | `CreatePetRequest`, `UpdatePetRequest`                |
| DTOs (input, domain)   | `<VerbPhrase>Request`              | `ScheduleAppointmentRequest`, `RegisterClinicRequest` |
| DTOs (output, full)    | `XResponse`                        | `UserResponse`, `AppointmentResponse`                 |
| DTOs (output, summary) | `XSummaryResponse`                 | `PetSummaryResponse`, `UserSummaryResponse`           |
| Mappers                | `XMapper`                          | `UserMapper`, `PetMapper`                             |
| Command objects        | `XCommand`                         | `ScheduleAppointmentCommand`                          |
| Exceptions             | Descriptive noun                   | `DuplicateEmailException`                             |
| Validators             | `XValidator`                       | `AppointmentValidator`                                |

No abbreviations unless universally known (`id`, `url`, `dto`).
No generic names: `XDto`, `XData`, `XInfo`, `XObject`.

---

## Entity Rules

### Structure

- Extend `AuditableEntity` for auditing and soft-delete support
- JPA annotations only (`@Entity`, `@Column`, `@ManyToOne`, etc.)
- Jakarta persistence constraints on columns (`@Column(nullable = false)`, `@Column(unique = true)`)
- No Jakarta validation annotations for business rules (those belong on DTOs)

### equals / hashCode

Every entity must implement `equals()` and `hashCode()` manually, based on the `id` field.
When `id` is `null` (transient entity), fall back to `super.equals()` / `super.hashCode()`.

```java

@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MyEntity other)) return false;
    return id != null && id.equals(other.getId());
}

@Override
public int hashCode() {
    return getClass().hashCode();
}
```

### Lombok on Entities

**Allowed:** `@Getter`, `@Setter`, `@NoArgsConstructor`

**Forbidden:** `@Data`, `@EqualsAndHashCode`, `@ToString`, `@Builder`

- `@EqualsAndHashCode` generates implementations incompatible with JPA proxy behavior
- `@ToString` risks `LazyInitializationException` and infinite recursion on bidirectional relationships
- `@Builder` conflicts with JPA's requirement for a no-arg constructor and does not work well with inheritance

### Bidirectional Relationships

Synchronization helpers in setters are acceptable only if `equals()` / `hashCode()` are properly implemented.
Prefer managing the owning side explicitly in services rather than relying on setter-based synchronization.

---

## DTO Rules

Controllers never accept or return JPA entities. All controller I/O uses DTOs.

### Naming

**Request DTOs (input):**

- `CreateXRequest`, `UpdateXRequest` for standard CRUD operations
- `<VerbPhrase>Request` for domain-specific operations that don't map to simple create/update semantics:
  `ScheduleAppointmentRequest`, `RescheduleAppointmentRequest`, `RegisterClinicRequest`
- The verb must reflect the domain operation, not the HTTP method

**Response DTOs (output):**

- `XResponse` for full detail views
- `XSummaryResponse` for lists and selectors — only the fields the view actually needs

**Never:** `XDto`, `XData`, `XBean`, `XInfo`, `XObject`

### Structure

- Input DTOs live in `<domain>/dto/request/`
- Output DTOs live in `<domain>/dto/response/`

### Lombok vs Java Records

**Request DTOs use Lombok** (`@Getter @Setter @NoArgsConstructor`). Thymeleaf's `@ModelAttribute`
binding requires a mutable bean with a no-arg constructor and setter methods — Java records are
immutable and incompatible with form binding.

**Response DTOs may use records** when they are only read in templates (no form binding). In practice,
Lombok classes are also acceptable for response DTOs to keep the codebase consistent.

**When to use a record:**

- Response-only DTOs that are never used as `@ModelAttribute` targets

**When to use a Lombok class:**

- All request DTOs (required for Thymeleaf form binding)
- Response DTOs that embed nested mutable objects
- The DTO requires inheritance (records are implicitly `final`)

**Record example:**

```java
public record PetResponse(
        Long id,
        String name,
        Species species,
        Sex sex,
        String breed,
        LocalDate birthDate,
        String ownerFullName
) {
}
```

**Request DTO with validation (Lombok class, bindable by Thymeleaf):**

```java
@Getter
@Setter
@NoArgsConstructor
public class CreatePetRequest {

  @NotBlank
  private String name;

  @NotNull
  private Species species;

  @NotNull
  private Sex sex;

  @NotNull
  private Breed breed;

  @NotNull
  @PastOrPresent
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate birthDate;
}
```

### Validation

- Jakarta Validation annotations (`@NotBlank`, `@Email`, `@FutureOrPresent`, etc.) on DTO fields
- `@Valid` on controller method parameters
- `LocalDate` fields bound from forms carry `@DateTimeFormat(iso = ISO.DATE)`, so the value is
  rendered and parsed as `yyyy-MM-dd`: the only format an HTML5 `<input type="date">` loads
- Entities carry only persistence constraints, not business validation

### Mapping

- Manual mapping in dedicated mapper utility classes — one mapper per domain entity
- Mappers are **non-instantiable utility classes**: `private` no-arg constructor that throws
  `UnsupportedOperationException`; all methods are `public static`
- No MapStruct, no reflection-based mappers unless explicitly approved
- Never use `BeanUtils.copyProperties()` — it copies audit fields silently and breaks with entity inheritance
- Mappers are called **only from controllers** — never from services or repositories

**Standard mapper methods:**

| Method                      | Direction     | Use                                 |
|-----------------------------|---------------|-------------------------------------|
| `toCreateCommand(XRequest)` | DTO → Command | Before passing to service on create |
| `toUpdateCommand(XRequest)` | DTO → Command | Before passing to service on update |
| `toResponse(Entity)`        | Entity → DTO  | Detail views                        |
| `toSummary(Entity)`         | Entity → DTO  | Lists and selectors                 |
| `toUpdateRequest(Entity)`   | Entity → DTO  | Pre-populating edit forms           |

---

## Service Rules

### Transactions

- `@Transactional(readOnly = true)` at class level for read-heavy services
- `@Transactional` on individual write methods
- Never place `@Transactional` on controllers or repositories

### Return Types

- Single-entity lookups return `Optional<T>` — never return `null`
- Use `orElseThrow(() -> new ResourceNotFoundException(...))` for mandatory lookups
- Never call `.get()` on `Optional` without checking

### Ownership Verification

Services that operate on user-owned resources (appointments, pets, visits, medical records) must verify
that the authenticated user has access to the resource before performing the operation.

### Layer Boundaries

Services must not import from the `dto.*` package. DTOs are a presentation concern; importing them
into the service layer couples the domain to the presentation contract and inverts the dependency
direction.

**What services accept as parameters:**

| Scenario                                                         | Service receives                                                | Who does the conversion                 |
|------------------------------------------------------------------|-----------------------------------------------------------------|-----------------------------------------|
| Simple CRUD                                                      | `Entity` (mapped from DTO by controller via mapper)             | Controller                              |
| Few independent params (≤ 3)                                     | Explicit primitives: `Long id`, `LocalDate date`, `String name` | Controller                              |
| Complex operation (> 3 params, or params span multiple entities) | `XCommand` (defined in `<domain>/command/`)                     | Controller maps `XRequest` → `XCommand` |

**Command objects (`XCommand`):**

- Plain POJO — no Jakarta Validation annotations, no JPA annotations, no Spring annotations
- Lives in `<domain>/command/` alongside the domain's other sub-packages
- Represents the service's own input contract, decoupled from the presentation DTO
- Use `@Builder` (Lombok) is acceptable here — commands are not JPA entities

**Services always return entities.** The controller is responsible for mapping returned entities
to the appropriate response DTO.

### Pagination

All service methods that return lists of entities must support pagination using Spring Data's `Pageable` /
`Page<T>`:

- Service methods accept `Pageable` as their last parameter
- Service methods return `Page<Entity>` — never `List<Entity>` for endpoints with potentially unbounded results
- Controllers receive pagination parameters automatically via Spring's `Pageable` argument resolver
- Default page size: 10. Maximum page size: 50. Configure via `@PageableDefault` on controller parameters.

```java
// Service interface
Page<Pet> findByOwnerId(Long ownerId, Pageable pageable);

// Controller
@GetMapping("/pets")
public String listPets(@PageableDefault(size = 10, sort = "name") Pageable pageable, Model model) {
    Page<Pet> page = petService.findByOwnerId(currentUserId(), pageable);
    model.addAttribute("pets", page.map(PetMapper::toSummary));
    return "pets/list";
}
```

Small, bounded collections (e.g., enum lookups, roles for a dropdown) may return `List<T>` directly.

---

## Repository Rules

- JPA repository interfaces only — no implementation classes
- Co-located with the domain (`user/repository/`, `pet/repository/`, etc.)
- Custom queries use `@Query` with JPQL or `@EntityGraph`
- No business logic in repository methods
- No duplicate methods — one method per query, one name per intent

---

## Controller Rules

- Thin: receive request, delegate to service, return view name or redirect
- All input via DTOs with `@Valid`
- All output via DTOs or response objects added to `Model`
- No `try/catch` blocks — exceptions are handled by `@ControllerAdvice`
- No direct entity manipulation — always go through service layer
- Use `@PreAuthorize` for method-level security when URL patterns are insufficient

### POST-Redirect-GET

Every write operation (`POST`, `PUT`, `DELETE`) must redirect after success to prevent duplicate submissions
on browser refresh. Use `RedirectAttributes.addFlashAttribute()` to pass feedback messages to the redirected
view.

```java

@PostMapping("/pets")
public String createPet(@Valid CreatePetRequest request, BindingResult result,
                        RedirectAttributes flash) {
    if (result.hasErrors()) {
        return "pets/create";  // re-render form — no redirect
    }
    petService.create(PetMapper.toCreateCommand(request));
    flash.addFlashAttribute("successMessage", "Pet registered successfully.");
    return "redirect:/pets";
}
```

- On **validation errors**: re-render the form (return view name, not redirect)
- On **success**: always `return "redirect:/<path>"`
- Flash attributes survive exactly one redirect and are automatically cleared

---

## Exception Handling

### Domain Exceptions

All business errors use domain-specific exception types in `exception/domain/`. Never throw:

- Raw `RuntimeException`
- `IllegalArgumentException` for business rule violations (use `BusinessRuleException`)
- `jakarta.persistence.EntityNotFoundException` (use `ResourceNotFoundException`)
- `java.nio.file.AccessDeniedException` (use Spring Security's or `UnauthorizedClinicAccessException`)

The domain exceptions and their hierarchy:

- `ResourceNotFoundException` — a mandatory lookup found nothing.
- `BusinessRuleException` — a business rule or input precondition was violated. Base type for:
    - `DuplicateEmailException` — registering/updating with an email already in use.
- `UnauthorizedClinicAccessException` — access to a resource outside the user's clinic.

All extend `RuntimeException`. Validators (`BaseValidator` and its subclasses) throw
`BusinessRuleException`; adding a new business rule does not require touching
`GlobalExceptionHandler`.

### Global Handler

A single `@ControllerAdvice` (`GlobalExceptionHandler`) handles all exceptions. Handlers that
always render a view carry `@ResponseStatus`; handlers that branch on the HTTP method set the
status manually (via `HttpServletResponse`) only on the GET branch, so the POST redirect keeps
its 302.

| Exception                                                       | POST                                        | GET                                   |
|-----------------------------------------------------------------|---------------------------------------------|---------------------------------------|
| `ResourceNotFoundException`                                     | flash `error` + redirect to `Referer` (302) | `error/404` view, **404**             |
| `BusinessRuleException` (incl. `DuplicateEmailException`)        | flash `error` + redirect to `Referer` (302) | `error/400` view, **400**             |
| `UnauthorizedClinicAccessException`, `AccessDeniedException`     | `error/access_denied` view, **403**         | `error/access_denied` view, **403**   |
| `NoResourceFoundException`, `MethodArgumentTypeMismatchException`| `error/404` view, **404**                   | `error/404` view, **404**             |
| Unhandled `Exception`                                           | `error/500` view, **500**                   | `error/500` view, **500**             |

- On **POST**, business/not-found errors redirect to the `Referer` (or `/`) with the message in a
  flash attribute named `error`. This follows Post-Redirect-GET and **intentionally discards the
  submitted form data** — the user re-enters the form. This trade-off is accepted: it keeps the
  handler uniform and avoids re-populating forms from the advice layer.
- The destination view must render the `error` flash via the `errorAlert` fragment
  (`fragments/alert.html`).
- On **GET**, the message is shown on the dedicated `error/400` or `error/404` page.
- The `Exception` handler logs a correlation UUID and never exposes a stack trace to the user.

Controllers must not catch exceptions manually. If a controller needs to re-render a form on
validation errors, use `BindingResult` — not `try/catch`.

---

## Security

### URL-Pattern Security

Configured in `SecurityConfig` via `authorizeHttpRequests()`.
Explicitly configure CSRF with `.csrf(Customizer.withDefaults())`.

### Method-Level Security

`@EnableMethodSecurity` is active. Use `@PreAuthorize` in services for:

- Ownership verification (user can only access their own resources)
- Role-based access that is too granular for URL patterns

### Resource Ownership Authorization

Ownership and clinic-scoped checks live in `shared/authorization/ResourceAuthorizationService`,
registered as `@Component("authz")`. The checks are declared with `@PreAuthorize` on the
**service interface** methods, referencing the bean by name:

```java
@PreAuthorize("@authz.canManageMedicalService(authentication, #serviceId)")
MedicalService update(Long serviceId, UpdateMedicalServiceCommand command, Long clinicId);
```

Write endpoints are also restricted by role in `SecurityConfig`, with specific matchers placed
before the general ones (for example, `/visits/*/edit` before `/visits/**`). Both layers apply:
the URL rule and the `@PreAuthorize` in the service.

- Each `canXxx` method returns a `boolean` (never throws to hide existence). A `false` result
  makes Spring Security raise `AccessDeniedException`, which the handler renders as a **403**.
- **Read and write are authorized separately.** Read operations use `canAccess*`; write
  operations use `canModify*`. Never reuse a `canAccess*` check on a write method.
- `SYSTEM_ADMINISTRATOR` passes every `canAccess*` check. The `canModify*` checks are
  `VETERINARIAN`-only: the administrator, `CUSTOMER` and `RECEPTIONIST` are rejected.
- `canManageMedicalService` returns `true` only for an administrator or a veterinarian whose own
  or work clinic owns the service — `CUSTOMER` and `RECEPTIONIST` are rejected.
- For pets, medical records and visits, **any appointment at the veterinarian's clinic grants the
  veterinarian access — including a cancelled one. This rule is transitory**: a veterinarian who has
  ever had the pet booked at their clinic keeps clinical read access. Today a clinic can create
  appointments without the owner's intervention, which would let it fabricate this access; it is
  planned that clinic-created appointments require the owner's confirmation before granting access
  (as the README roadmap indicates).
- `RECEPTIONIST` has no access to medical records or visits: `canAccessMedicalRecord` and
  `canAccessVisit` return `false` for that role, in addition to the URL rules.
- `canModifyMedicalRecord` (general observations) follows the clinic rule above.
- **Visits follow the author rule:** `canModifyVisit` returns `true` only for the veterinarian who
  recorded the visit, and only while their clinic still has an appointment for the pet. Colleagues
  at the same clinic cannot edit or remove it. If the author leaves the clinic, the visit is
  corrected by recording a new visit.
- Views that offer write actions get the decision as a model flag computed by the controller
  with the same `canModify*` check (for example, `canEditVisit`); templates never repeat the
  rule.
- Invariants between related entities (pet/customer, medical service/clinic) are verified in the
  service, never only in the DTO: any ID that arrives in a form can be tampered with.

### Authentication

- Soft-deleted users (`active = false`) must not be able to authenticate
- `AuthenticatedUser.isEnabled()` must return `false` for inactive users
- Spring Security's `UserDetailsService` must reject inactive users explicitly

### Mass Assignment Prevention

Never bind JPA entities directly from form input. Use DTOs with only the fields the form should set.
Services must enforce invariants (e.g., forced roles) regardless of input.

---

## Logging

- SLF4J via Lombok's `@Slf4j` on all service implementations
- Log at service boundaries only — not in entities, not in repositories
- Levels:
    - `INFO`: business events (user created, appointment cancelled, clinic registered)
    - `DEBUG`: technical detail (query parameters, cache hits)
    - `WARN`: recoverable issues (duplicate email attempt, invalid state transition)
    - `ERROR`: unexpected failures (unhandled exceptions, external service failures)
- Never log: passwords, tokens, personal data, full stack traces at INFO level
- Email is never logged in full: use `tfg.psygcv.shared.logging.LogSanitizer.maskEmail` (keeps the
  first character of the local part and the domain, e.g. `j***@dominio.com`) or replace it with the
  user ID. A value with no `@` is masked in full, since it may be a password typed by mistake; so
  is a value whose domain contains anything other than letters, digits, dots and hyphens, so text
  injected after the `@` never reaches the log.
- Any user-supplied value that reaches a log must be passed through
  `LogSanitizer.sanitize`, which strips control characters (including `\r` and `\n`) so a value
  cannot forge extra log lines.
- Do not reveal whether an email is registered: a failed login always shows the same redirect and
  the same generic message, regardless of whether the email exists. The failure handler may log the
  masked email and the simple name of the exception class.
- Login events: successful login logs `INFO` with the user ID and role (no email); failed login
  logs `WARN` with the masked email and the reason (exception simple name).

---

## Date and Time

- The current date and time always come from the injected `Clock` bean (`config/time/ClockConfig`):
  `LocalDate.now(clock)`, `LocalDateTime.now(clock)`. Never call `now()` without arguments — it
  uses the JVM default time zone
- Inject the `Clock` by constructor, like any other dependency
- The application time zone is defined by the `app.time-zone` property in `application.yaml`
  (`America/Argentina/Cordoba`). The JVM may run in UTC: business dates do not depend on it, and log
  timestamps stay in the JVM zone
- The same `Clock` backs JPA auditing (`@CreatedDate`, `@LastModifiedDate`, through the
  `DateTimeProvider` in `JpaAuditConfig`) and Bean Validation (`@FutureOrPresent`, `@PastOrPresent`,
  through a `ClockProvider`)
- Entities cannot inject the `Clock`: `AuditableEntity` takes `deletedAt` from `updatedAt`, which
  `AuditingEntityListener` sets before the entity's own `@PreUpdate` callback
- `hibernate.jdbc.time_zone` is not configured: persisted types carry no zone (`LocalDate`,
  `LocalTime`, `LocalDateTime`) and queries use no database date functions. Revisit it if either
  changes

---

## Database

### Flyway Migrations

- Flyway manages the schema in every profile (`dev`, `test`, `prod`)
- Location: `src/main/resources/db/migration/`
- Every schema change is a new migration named `V{n}__description.sql` (`V2__add_pet_microchip.sql`)
- Never modify a migration that has already been applied — create a new one
- Migrations contain no application or seed data; transforming existing rows is allowed when a
  schema change requires it (for example, a backfill before adding `NOT NULL`)
- Constraint names are readable: `uk_<table>_<column>` for unique constraints and
  `fk_<table>_<column>` for foreign keys
- `V1__baseline.sql` was generated from the entity model with Hibernate's schema-generation scripts
  (MySQL dialect). It does not declare a charset or collation: they are set when the database is
  created
- Databases start empty: Flyway does not baseline an existing schema (no `baseline-on-migrate`). A
  database created before Flyway (by `ddl-auto`) must be recreated empty
- `ddl-auto: validate` only detects missing tables or columns and clearly incompatible types. Types
  of the same family (for example, `varchar` and `TEXT`) are considered compatible, and it does not
  compare length, nullability, uniqueness, foreign keys or enum values. Any of those changes in an
  entity still requires a migration
- `SchemaDriftTest` compares the migrated schema with the one Hibernate generates from the entities,
  so it detects a missing or divergent migration, including what `validate` does not see: length,
  nullability, uniqueness, foreign keys and enum values. After a Hibernate upgrade it may fail
  because the generator changed: fix it with a new migration, never by editing an applied one
- Adding, removing or renaming a constant of a persisted enum requires a migration that modifies the
  column: Hibernate maps `@Enumerated(EnumType.STRING)` to a native MySQL `enum(...)` column, and
  `ddl-auto: validate` does not detect the difference

### Hibernate

- `ddl-auto: validate` in every profile — never `update`, `create` or `create-drop`
- `open-in-view: false` explicitly
- SQL logging is enabled only by the `debug` profile, through the `org.hibernate.SQL` and
  `org.hibernate.orm.jdbc.bind` loggers
- `show-sql` is not enabled in any profile — it duplicates the logger output

---

## Spring Profiles

| Profile | Database                  | DDL        | Flyway     | Notes                                                               |
|---------|---------------------------|------------|------------|---------------------------------------------------------------------|
| `dev`   | MySQL                     | `validate` | Migrates   | Local development. The database is created empty                    |
| `test`  | MySQL (Testcontainers)    | `validate` | Migrates   | Test runs and CI. Requires Docker                                   |
| `prod`  | MySQL                     | `validate` | Migrates   | Production. Credentials from environment variables                  |
| `debug` | (inherits)                | (inherits) | (inherits) | SQL and bound-parameter logging. Local only; combine as `dev,debug` |

Local credentials live in `application-local.yaml` at the project root: not versioned, imported by the
`dev` profile.

No hardcoded credentials in any profile. Never modify `application-prod.yaml` unless explicitly instructed.

---

## Docker

- **Images are pinned by full version tag and digest** (`image:tag@sha256:...`): Eclipse Temurin 25
  (JDK for the build, JRE for the runtime) and MySQL 8.4. Dependabot keeps the `docker` and
  `docker-compose` ecosystems updated.
- **The MySQL version used by the tests follows the Compose one.** `TestcontainersConfiguration`
  pins the same full version as `docker-compose.yml`; when a Dependabot PR updates the MySQL image in
  Compose, that same PR must update the test constant, because Dependabot does not detect it.
- **The application image is multi-stage.** The build stage copies only `pom.xml`, `.mvn/`, `mvnw`
  and `src/` — never `COPY . .` — runs `./mvnw -B -ntp package -Dmaven.test.skip=true` and caches the
  Maven repository with a BuildKit cache mount. Tests need Docker and run in the CI, not in the image
  build.
- **The runtime is a JRE**, not a JDK. The jar is extracted with Spring Boot's `tools` jarmode and
  copied in layers; `ENTRYPOINT` uses the exec form.
- **The container runs as a non-root user.** Files under `/application` stay owned by root and
  read-only for that user: the application never modifies its own code. If another owner is ever
  needed, use `COPY --chown`; never `RUN chown` over application files.
- **The heap is sized from the container limit** with
  `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75`; each environment can override it.
- **No secret or local file enters the image.** `.dockerignore` excludes `application-local.*`,
  `.env*`, `.git`, `target/`, the IDE and tooling directories and the project plans; `COPY` is always
  explicit.
- **Compose runs the application with the `prod` profile** (`SPRING_PROFILES_ACTIVE` from `.env`).
  The four MySQL variables are required and fail fast with `${VAR:?message}`; the five `ADMIN_*`
  variables are optional and all-or-none.
- **Compose publishes ports only on `127.0.0.1`**, sets a memory limit for both services and runs the
  application with `no-new-privileges` and `cap_drop: ALL`. The database port is never published.
- **Healthchecks:** MySQL uses `mysqladmin ping` over TCP; the application queries
  `/actuator/health` with bash and `/dev/tcp`, because the JRE image has no `curl` or `wget` and none
  is installed, to keep the image small.
- **Actuator has two layers of protection.** `management.endpoints.web.exposure.include: health`
  exposes only `health` (with `show-details: never`), and `SecurityConfig` permits
  `GET /actuator/health` while denying `/actuator/**` to everyone, including administrators. An
  endpoint added to the exposure by mistake therefore does not become public.
- **Graceful shutdown is Spring Boot's default**; Docker's stop grace period (40 s) is above the
  30 s Spring waits per shutdown phase.

---

## Application Bootstrap

Startup data that cannot be created through the web UI (currently only the initial administrator)
follows this pattern:

- **Configuration** is bound from environment variables into a `@ConfigurationProperties` class in
  `config/bootstrap/`. Defaults are empty, so tests, CI and development need no variables.
- **All or nothing:** if some variables are defined while others are missing or blank, the
  configuration class fails during context refresh with a message naming the missing variables, and
  the application does not start. Never include the values. A blank value counts as not defined
  (`StringUtils.hasText`).
- **The rule lives in the service:** the runner is a thin `ApplicationRunner` that only reads the
  configuration and calls a dedicated service method (for example,
  `createInitialAdministratorIfAbsent`). That method is idempotent, is not exposed by any controller
  and does not reuse administration operations meant for authenticated users.
- **Idempotent by rule:** it creates the initial administrator only when no active
  `SYSTEM_ADMINISTRATOR` exists, so restarting with the same variables is safe.
- **Never log secrets or personal data:** the bootstrap logs the created user ID only, never the
  email or the password.

### Last active administrator

The system must never be left without an active `SYSTEM_ADMINISTRATOR`: the last one cannot be
deactivated or lose the administrator role. The invariant is enforced in a single point in
`UserServiceImpl` (`removesLastActiveAdministrator`), shared by `deactivate` and `updateComplete`;
violations throw `BusinessRuleException`. The count of active administrators is queried only when the
change actually deactivates an administrator or removes its role, and a `null` `active` value means
"no change". Every path that changes a user's active state or role must go through those two
methods.

---

## Testing

### What to Test

| Type         | Annotation        | Scope                                             |
|--------------|-------------------|---------------------------------------------------|
| Service unit | Mockito           | Business logic, validation, state transitions     |
| Controller   | `@WebMvcTest`     | Request mapping, validation, security             |
| Repository   | `@DataJpaTest`    | Custom queries, `@SQLRestriction`, `@EntityGraph` |
| Integration  | `@SpringBootTest` | Cross-layer flows (use sparingly)                 |

### What NOT to Test

- Framework internals, getters/setters, trivial mappings
- Lombok-generated code
- Spring Data derived query methods (tested by the framework)

### Mockito

- Constructor injection preferred
- No mocking entities or value objects
- Avoid over-mocking — if everything is mocked, the test proves nothing
- `lenient()` is allowed only for stubs that exist solely to make a regression fail a test: with
  the correct code the branch under test short-circuits before the stub is used, and strict
  stubbing would otherwise report it as unnecessary. Always add a `// Lenient: ...` comment
  explaining why the stub exists.

### Test Support Classes

- Shared fixtures for authentication live in `src/test/java/tfg/psygcv/support/` (for example,
  `AuthTestFactory`), reusable across the test suite.
- Build the authenticated principal with `AuthTestFactory`: `user(id, role)` creates a real `User`
  and `authentication(user)` wraps `AuthenticatedUser.from(user)` in an authenticated
  `UsernamePasswordAuthenticationToken`, mirroring what Spring Security provides at runtime. Never
  build the principal by hand inside a test.
- Fixture factories (such as `AuthTestFactory`) are non-instantiable utilities (private
  constructor), matching the mapper pattern. The rule does not apply to Spring test configurations
  (such as `TestcontainersConfiguration`): Spring must be able to instantiate them.

### Database in Tests

- Tests that load a Spring context run against MySQL 8.4 in a container: they import
  `TestcontainersConfiguration` (`support/`), which declares the `MySQLContainer` bean with
  `@ServiceConnection`. The image version is defined only there
- Flyway applies the migrations on startup and `ddl-auto: validate` checks the entities against
  them, so `./mvnw clean verify` fails if an entity references a missing table or column
- Docker is required to run these tests. Mockito unit tests do not need it
- The container lives as long as the Spring context: test classes that share the same cached
  context share one container, but a test with a different context configuration (for example, a
  slice test such as `@DataJpaTest`) starts its own container

### Runtime Verification

To verify a change in the running application, use the `dev` profile against a real MySQL (local or
in a container), never the `test` profile.

### Test Naming

Use the pattern `should_expectedBehavior_when_condition`:

```java

@Test
void should_throwDuplicateEmailException_when_emailAlreadyExists() { ...}

@Test
void should_returnPage_when_ownerHasPets() { ...}

@Test
void should_rejectAccess_when_userIsNotOwner() { ...}
```

- Start with `should_` — it reads as a specification
- The `when_` clause describes the scenario under test
- Use camelCase within each segment
- No `test` prefix — JUnit Jupiter does not require it

### Assertions

- Use AssertJ fluent assertions over JUnit's `assertEquals`
- One logical assertion per test method (multiple `assertThat` calls are fine if they verify the same behavior)

---

## Thymeleaf Templates

- Location: `src/main/resources/templates/`, organized by feature
- Reusable components in `fragments/`
- Base layout: `layouts/layout.html` — all pages extend it
- Use `th:replace` / `th:insert` for shared components
- Always use `th:action` on forms (ensures CSRF token inclusion)
- Use `th:text` for user content — never `th:utext` with unescaped input
- Keep templates thin — prepare all data in controllers via DTOs

---

## Documentation

- The README exists in two languages: `README.md` (English) and `README.es.md` (Spanish)
- `README.md` is the reference version; `README.es.md` is a faithful translation with the same
  structure, diagrams and links
- Both files are updated in the same PR: a change to one without the other is incomplete

---

## Git Conventions

### Commit Messages (Conventional Commits)

Format: `<type>(<scope>): <description>`

Types: `feat`, `fix`, `refactor`, `perf`, `test`, `docs`, `style`, `build`, `ci`, `chore`, `revert`

Rules:

- Imperative mood: "add user registration" not "added"
- Max 72 characters in subject line (aim for 50-60)
- Scope recommended: `(auth)`, `(user)`, `(pet)`, `(medical)`, `(appointment)`, `(clinic)`
- Body explains **why**, not what (encouraged for non-trivial changes)
- No emojis. No vague words: `update`, `WIP`, `changes`, `minor`

### Branch Naming

Format: `<type>/<short-description>`

- `feat/user-profile-avatar`, `fix/appointment-date-validation`, `hotfix/auth-bypass`
- Kebab-case. Prefix matches commit type.

### PR Titles

Conventional Commits format: `feat(user): implement profile page with avatar upload`

---

## Forbidden Actions

- Rewrite large parts of the codebase without explicit approval
- Change the package structure without explicit approval
- Introduce new architectural patterns (CQRS, event sourcing, etc.)
- Add dependencies to `pom.xml` without being asked
- Add Lombok annotations beyond the allowed list
- Silence warnings or skip checks instead of fixing root causes
- Use any `ddl-auto` value other than `validate` in any profile
- Modify a Flyway migration that has already been applied
- Expose JPA entities through controller signatures
- Catch exceptions in controllers (use `@ControllerAdvice`)
- Return `null` from service methods (use `Optional`)
- Use `BeanUtils.copyProperties()` for entity mapping
- Import from `dto.*` in the service or repository layer
- Call mapper methods from services or repositories — mappers belong in the controller layer
- Log passwords, tokens, or personal data
