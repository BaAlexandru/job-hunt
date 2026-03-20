# Project Conventions

## Architecture Rules

1. Backend-first development: API built and tested before frontend
2. Gradle multi-project: plugins declared in root with `apply false`, applied in subprojects without versions
3. No `subprojects {}` block in root build.gradle.kts
4. Each phase adds only the dependencies it needs

## Kotlin Rules

1. Use data classes for DTOs and value objects
2. Use constructor injection via primary constructors, never @Autowired fields
3. Use Kotlin null safety instead of Optional<T>
4. Use `when` expressions instead of if-else chains for type/enum matching
5. No Lombok -- Kotlin language features replace it entirely

## Database Rules

1. Flyway manages ALL schema changes
2. hibernate.ddl-auto must be `validate`, never `update` or `create-drop`
3. Migration naming: V{N}__{phaseNN}_{description}.sql
4. UUID primary keys with gen_random_uuid() default
5. All tables include created_at TIMESTAMPTZ and updated_at TIMESTAMPTZ columns
6. Multi-user ready: all domain tables have a user_id foreign key

## API Rules

1. All domain endpoints under /api/ prefix
2. RESTful resource naming: /api/companies, /api/jobs, /api/applications
3. Return appropriate HTTP status codes: 201 for create, 204 for delete, 400 for validation, 404 for not found
4. Use data classes for request/response bodies
5. Actuator endpoints at /actuator/ (health, info, flyway) — do not create custom health endpoints

## Testing Rules

1. @SpringBootTest for integration tests
2. @WebMvcTest(Controller::class) for controller unit tests
3. Test files mirror source structure in src/test/kotlin/
4. Test class naming: {ClassName}Tests.kt

## Git Workflow Rules

1. Each phase executes on its own branch: `phase-{NN}-{slug}` (e.g., `phase-02-auth`)
2. Create the branch from master BEFORE `/gsd:execute-phase` begins
3. All implementation commits go to the phase branch, never directly to master
4. After phase execution, open a PR from the phase branch into master
5. PR must be reviewed — apply requested changes before merging
6. Planning docs (RESEARCH.md, PLAN.md, CONTEXT.md, VALIDATION.md) may commit to master
7. Never force-push to master

## Docker Rules

1. compose.yaml at project root (not in /infra) for Spring Boot auto-discovery
2. No database connection properties in application.yml (docker-compose auto-configures)
3. Fixed port mappings, not random
4. Named volumes for data persistence
