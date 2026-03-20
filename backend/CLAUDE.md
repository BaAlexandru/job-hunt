# Backend - Claude Code Instructions

## Stack

- Kotlin 2.2.21 with Java 24 toolchain
- Spring Boot 4.0.4 (uses renamed starters: webmvc, not web)
- Spring Data JPA with PostgreSQL
- Flyway for database migrations
- JUnit 5 + Spring Boot Test

## Package Structure

Base package: com.alex.job.hunt.jobhunt
- /controller - REST controllers (@RestController)
- /service - Business logic (@Service)
- /repository - Data access (@Repository, extends JpaRepository)
- /entity - JPA entities (@Entity)
- /dto - Request/response data classes

## Coding Conventions

- Use Kotlin idioms: data classes, extension functions, null safety, when expressions
- Entity classes: use @Entity annotation (allOpen plugin handles making them open)
- No Lombok -- use Kotlin language features instead
- API endpoints under /api/ prefix (e.g., /api/users, /api/companies)
- Return data classes from controllers (Jackson Kotlin module handles serialization)
- Use constructor injection (Kotlin primary constructors), never @Autowired

## Database

- Flyway manages ALL schema changes, never hibernate ddl-auto
- Migration naming: V{N}__{phaseNN}_{description}.sql
- UUID primary keys: use gen_random_uuid() as default in SQL
- All tables should have created_at and updated_at timestamp columns

## Testing

- @SpringBootTest for integration tests (loads full context, starts Docker Compose)
- @WebMvcTest for controller-only tests (no database)
- Test command: ../gradlew test (from backend/) or ./gradlew :backend:test (from root)

## Actuator

- Spring Boot Actuator provides production-ready monitoring endpoints
- Exposed endpoints (no auth required yet — add security in Phase 2):
  - GET /actuator/health — application health with DB, Flyway, and disk space indicators
  - GET /actuator/info — application info
  - GET /actuator/flyway — applied Flyway migrations
- Health details visible via `show-details: always` (safe for single-user local dev)
- When Spring Security is added (Phase 2), restrict actuator to authenticated users
- Do NOT expose /actuator/env or /actuator/configprops without auth (leak secrets)

## Spring Boot 4.0 Specifics

- Starters renamed: use spring-boot-starter-webmvc (not starter-web)
- Jackson group: tools.jackson.module (not com.fasterxml.jackson)
- Flyway: use spring-boot-starter-flyway (not raw flyway-core)
- Modular starters: each feature needs its own starter dependency
