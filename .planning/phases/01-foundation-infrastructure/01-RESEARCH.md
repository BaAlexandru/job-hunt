# Phase 1: Foundation & Infrastructure - Research

**Researched:** 2026-03-19
**Domain:** Gradle multi-project build, Spring Boot 4.0, Docker Compose, Flyway, Kotlin
**Confidence:** HIGH

## Summary

Phase 1 restructures an existing flat Spring Boot project into a Gradle multi-project monorepo with `/backend`, `/frontend`, and `/infra` directories. The Spring Boot application (4.0.4) already exists at the root with most dependencies configured -- the work is primarily structural reorganization, dependency trimming, Docker Compose enhancement, and Flyway baseline migration setup.

The existing `build.gradle.kts` already uses Spring Boot 4.0 naming conventions (e.g., `spring-boot-starter-webmvc` instead of the old `spring-boot-starter-web`, `tools.jackson.module:jackson-module-kotlin` instead of `com.fasterxml.jackson`). The Kotlin compiler plugins (`plugin.spring`, `plugin.jpa`) and `allOpen` configuration are already working. The primary technical challenge is correctly setting up the Gradle multi-project structure so that plugins are declared centrally but applied only to the `:backend` subproject.

**Primary recommendation:** Move the existing root-level Spring Boot app into `/backend` as a Gradle subproject, trim dependencies to Phase 1 needs only, enhance `compose.yaml` with fixed ports and named volumes, add a single Flyway baseline migration, and configure `application.yml` for PostgreSQL connectivity via Docker Compose service discovery.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Gradle multi-project build: root settings.gradle.kts includes `:backend`
- /frontend is a plain Next.js directory with pnpm (not a Gradle subproject)
- /infra contains Docker files only (Dockerfiles, no Helm/K8s yet)
- compose.yaml stays at project root for convenience and Spring Boot docker-compose auto-discovery
- PostgreSQL container only for local dev -- backend runs on host via Gradle for fast dev cycle
- Fixed host port 5432 mapping (not random)
- Named volume for PostgreSQL data persistence across container restarts
- Spring Boot docker-compose integration auto-discovers compose.yaml at root
- V1 creates schema extensions only (e.g., CREATE EXTENSION IF NOT EXISTS "pgcrypto" for gen_random_uuid())
- No domain tables in baseline -- each phase adds its own migrations
- Phase-prefixed naming convention: V1__phase01_baseline.sql, V2__phase02_create_users.sql, etc.
- UUID primary keys (gen_random_uuid()) for all tables
- Keep Spring Boot 4.0.4 + Spring AI 2.0.0-M3 (latest, accept milestone risks)
- Trim build.gradle.kts to Phase 1 needs only: spring-boot-starter-web, data-jpa, flyway, postgresql, kotlin-reflect, jackson-kotlin, devtools, docker-compose
- Remove Lombok entirely -- Kotlin's data classes and language features replace it
- Add back dependencies (Spring Security, Spring AI, OAuth2, restdocs, session-jdbc) when their phases start

### Claude's Discretion
- Exact Gradle multi-project configuration details
- .gitignore patterns for the monorepo
- Application properties structure (application.yml vs application.properties)
- CLAUDE.md file content and structure per module
- Test configuration and any health-check endpoint for verifying the app starts

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| INFR-01 | Application runs in Docker containers via Docker Compose | Docker Compose stack section; compose.yaml enhancement pattern; Spring Boot docker-compose auto-discovery |
| INFR-02 | PostgreSQL runs as a container in the compose stack | PostgreSQL container configuration with fixed port, named volume, and proper env vars |
| INFR-03 | Monorepo structure with /backend, /frontend, /infra directories | Gradle multi-project architecture pattern; settings.gradle.kts configuration |
| INFR-04 | Database migrations managed by Flyway | Flyway baseline migration pattern; spring-boot-starter-flyway dependency; V1 naming convention |
| DEVX-01 | Nested CLAUDE.md files per module (backend, frontend, infra) | Claude's discretion -- content patterns documented in Architecture Patterns |
| DEVX-02 | Dedicated project-level Claude Code skills | Claude's discretion -- .claude/skills/ directory structure |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 4.0.4 | Application framework | Already in project; user locked to this version |
| Kotlin | 2.2.21 | Language | Already configured; Spring Boot 4 requires Kotlin 2.2+ |
| Gradle | 9.3.1 | Build tool | Already configured via wrapper |
| PostgreSQL | latest (Docker image) | Database | User decision; runs as container |
| Flyway | via spring-boot-starter-flyway | Database migrations | Spring Boot 4 modular starter |

### Phase 1 Dependencies (trimmed)
| Library | Artifact | Purpose | Status |
|---------|----------|---------|--------|
| Spring Boot Web MVC | spring-boot-starter-webmvc | REST endpoints, health check | Keep (renamed from starter-web in SB4) |
| Spring Data JPA | spring-boot-starter-data-jpa | JPA/Hibernate ORM | Keep |
| Flyway Starter | spring-boot-starter-flyway | Migration auto-configuration | Keep (SB4 modular starter) |
| Flyway PostgreSQL | org.flywaydb:flyway-database-postgresql | PostgreSQL dialect for Flyway | Keep |
| PostgreSQL Driver | org.postgresql:postgresql | JDBC driver | Keep (runtimeOnly) |
| Kotlin Reflect | org.jetbrains.kotlin:kotlin-reflect | Kotlin reflection for Spring | Keep |
| Jackson Kotlin | tools.jackson.module:jackson-module-kotlin | JSON serialization | Keep (SB4 uses tools.jackson group) |
| Spring Boot DevTools | spring-boot-devtools | Hot reload | Keep (developmentOnly) |
| Docker Compose | spring-boot-docker-compose | Auto-discover compose.yaml | Keep (developmentOnly) |
| Kotlin Test JUnit5 | org.jetbrains.kotlin:kotlin-test-junit5 | Test framework | Keep |
| JUnit Platform | org.junit.platform:junit-platform-launcher | Test runner | Keep (testRuntimeOnly) |

### Dependencies to REMOVE in Phase 1
| Library | Reason |
|---------|--------|
| spring-boot-starter-data-jdbc | Redundant with JPA |
| spring-boot-starter-jdbc | Redundant with JPA |
| spring-boot-starter-restclient | Not needed until API consumption phase |
| spring-boot-starter-security | Phase 2 (Auth) |
| spring-boot-starter-security-oauth2-client | Phase 2 (Auth) |
| spring-boot-starter-session-jdbc | Phase 2 (Auth) |
| spring-ai-starter-* (all three) | Phase 8+ (AI) |
| spring-ai-bom | Phase 8+ (AI) |
| spring-ai-spring-boot-docker-compose | Phase 8+ (AI) |
| org.projectlombok:lombok (both compileOnly and annotationProcessor) | Kotlin replaces Lombok |
| spring-boot-configuration-processor | Not needed without @ConfigurationProperties |
| org.asciidoctor.jvm.convert plugin | Not needed until API docs phase |
| spring-boot-restdocs + spring-restdocs-mockmvc | Not needed until API docs |
| All *-test starters except base spring-boot-starter-test | Excessive; add as needed |

## Architecture Patterns

### Recommended Project Structure
```
job-hunt/                          # Root project
├── settings.gradle.kts            # include(":backend")
├── build.gradle.kts               # Root: plugin declarations with apply false
├── compose.yaml                   # Docker Compose (at root for auto-discovery)
├── .gitignore                     # Monorepo-wide ignores
├── CLAUDE.md                      # Root-level Claude instructions
├── .claude/
│   └── skills/                    # Project-level Claude Code skills (DEVX-02)
├── backend/
│   ├── build.gradle.kts           # Backend-specific: applies plugins, declares deps
│   ├── CLAUDE.md                  # Backend-specific Claude instructions
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/alex/job/hunt/jobhunt/
│       │   │   └── JobHuntApplication.kt
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       │           └── V1__phase01_baseline.sql
│       └── test/
│           └── kotlin/com/alex/job/hunt/jobhunt/
│               └── JobHuntApplicationTests.kt
├── frontend/                      # Plain Next.js directory (not Gradle subproject)
│   └── CLAUDE.md                  # Frontend-specific Claude instructions
└── infra/
    ├── CLAUDE.md                  # Infra-specific Claude instructions
    └── docker/                    # Dockerfiles (future phases)
```

### Pattern 1: Gradle Multi-Project with Spring Boot
**What:** Root project declares plugins with `apply false`; backend subproject applies them.
**When to use:** Always for this project structure.

Root `settings.gradle.kts`:
```kotlin
rootProject.name = "job-hunt"
include(":backend")
```

Root `build.gradle.kts`:
```kotlin
plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    kotlin("plugin.jpa") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.4" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}
```

`backend/build.gradle.kts`:
```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.alex.job.hunt"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

### Pattern 2: Docker Compose for Local Dev
**What:** PostgreSQL runs in Docker; backend runs on host with Gradle.
**When to use:** Local development workflow.

```yaml
# compose.yaml (at project root)
services:
  postgres:
    image: 'postgres:17'
    environment:
      POSTGRES_DB: jobhunt
      POSTGRES_USER: jobhunt
      POSTGRES_PASSWORD: jobhunt
    ports:
      - '5432:5432'
    volumes:
      - pgdata:/var/lib/postgresql/data

volumes:
  pgdata:
```

Key details:
- Fixed port `5432:5432` (not random) per user decision
- Named volume `pgdata` for data persistence across restarts
- Spring Boot docker-compose starter auto-discovers this file at root
- When running `./gradlew :backend:bootRun`, Spring Boot starts PostgreSQL container automatically
- No need to manually run `docker compose up` for the database

### Pattern 3: Application Configuration (application.yml)
**What:** YAML configuration for Spring Boot with profile-based overrides.
**Recommendation:** Use `application.yml` over `.properties` for readability with nested config.

```yaml
# backend/src/main/resources/application.yml
spring:
  application:
    name: job-hunt
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
```

Note: Database connection properties are NOT needed when using Spring Boot docker-compose integration -- it auto-configures JDBC URL, username, and password from the compose.yaml service definition.

### Pattern 4: Flyway Baseline Migration
**What:** First migration enables PostgreSQL extensions needed by the application.

```sql
-- V1__phase01_baseline.sql
-- Baseline migration: enable extensions for UUID generation
-- Note: gen_random_uuid() is built into PostgreSQL 13+, but pgcrypto
-- provides additional cryptographic functions we may need later.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

### Anti-Patterns to Avoid
- **Putting plugins block in subproject build.gradle.kts with versions:** Versions must be in root `plugins {}` block with `apply false`. Subprojects reference plugins without versions.
- **Using `hibernate.ddl-auto=update` or `create-drop`:** Flyway manages ALL schema changes. Set to `validate` to catch drift.
- **Hardcoding database connection properties:** Let Spring Boot docker-compose integration auto-configure them from compose.yaml.
- **Using `subprojects {}` block in root build.gradle.kts:** Modern Gradle recommends against cross-project configuration. Use `apply false` pattern instead.
- **Random Docker port mapping (`- '5432'`):** The existing compose.yaml maps a random host port. Must change to fixed `5432:5432`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Database migrations | Manual SQL scripts or Hibernate ddl-auto | Flyway via spring-boot-starter-flyway | Versioned, repeatable, team-safe migrations |
| Docker service discovery | Manual connection string management | spring-boot-docker-compose | Auto-configures JDBC from compose.yaml labels/ports |
| Kotlin class proxying for JPA | Manual open class declarations | allOpen plugin with JPA annotations | Spring/Hibernate require open classes; plugin handles it |
| Kotlin class proxying for Spring | Manual open class/method declarations | kotlin("plugin.spring") | Automatically opens @Component, @Service, etc. |
| JSON serialization for Kotlin | Custom serializers for data classes | jackson-module-kotlin | Handles Kotlin nullability, default params, data classes |

**Key insight:** Spring Boot 4.0's modular starter system means you get less "magic" by default but more explicit control. The `spring-boot-starter-flyway` starter is required -- raw `flyway-core` alone will NOT trigger auto-configuration in SB4.

## Common Pitfalls

### Pitfall 1: Missing Flyway Starter in Spring Boot 4
**What goes wrong:** Flyway migrations silently don't run. No error, just no migration execution.
**Why it happens:** Spring Boot 4 modularized starters. The old pattern of depending on `flyway-core` alone no longer triggers auto-configuration.
**How to avoid:** Use `spring-boot-starter-flyway` (not just `flyway-core`). The existing build.gradle.kts already has this correct.
**Warning signs:** Application starts but database has no tables/extensions.

### Pitfall 2: Gradle Plugin Version Conflicts in Multi-Project
**What goes wrong:** Build fails with "plugin was loaded multiple times" or version mismatch errors.
**Why it happens:** Declaring plugin versions in both root and subproject build.gradle.kts files.
**How to avoid:** Declare all plugins with versions in root `plugins {}` block with `apply false`. Subprojects use `plugins { id("...") }` without version.
**Warning signs:** Build warnings about multiple plugin classloaders.

### Pitfall 3: Spring Boot Docker Compose Not Finding compose.yaml After Restructure
**What goes wrong:** Application fails to start because it can't find the Docker Compose file.
**Why it happens:** After moving the app to `/backend`, the working directory changes. Spring Boot looks for compose.yaml relative to the working directory.
**How to avoid:** When running via `./gradlew :backend:bootRun` from root, the working directory is the project root, so compose.yaml at root is found. If running from IDE, may need `spring.docker.compose.file=../compose.yaml` in test/dev config.
**Warning signs:** "No Docker Compose file found" log message on startup.

### Pitfall 4: JPA Entity Classes Not Open in Kotlin
**What goes wrong:** Hibernate proxying fails with runtime errors or lazy loading doesn't work.
**Why it happens:** Kotlin classes are final by default. Hibernate needs open (non-final) classes for proxying.
**How to avoid:** The `allOpen` block in build.gradle.kts opens `@Entity`, `@MappedSuperclass`, and `@Embeddable` classes. The `plugin.spring` opens Spring stereotype annotations. Both are already configured.
**Warning signs:** `BeanCreationException` or `HibernateException` mentioning final classes.

### Pitfall 5: PostgreSQL pgcrypto Not Needed on PostgreSQL 13+
**What goes wrong:** Nothing technically breaks, but the migration is misleading.
**Why it happens:** `gen_random_uuid()` is a core function since PostgreSQL 13. The `pgcrypto` extension is no longer required for UUID generation.
**How to avoid:** The baseline migration should still include it for backward compatibility and because pgcrypto provides other useful functions. The `postgres:17` Docker image has gen_random_uuid() built in regardless.
**Warning signs:** None -- this is informational.

### Pitfall 6: Missing spring-boot-starter-test
**What goes wrong:** Test compilation fails because test utilities are missing.
**Why it happens:** The existing build has many individual `*-test` starters but not the base `spring-boot-starter-test`.
**How to avoid:** Replace all the individual test starters with just `spring-boot-starter-test` which includes JUnit, Mockito, AssertJ, Spring Test, and more.
**Warning signs:** Missing imports for @SpringBootTest, MockMvc, etc.

## Code Examples

### Health Check Endpoint (Verification that app starts)
```kotlin
// Source: Standard Spring Boot pattern
package com.alex.job.hunt.jobhunt

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/api/health")
    fun health(): Map<String, String> = mapOf("status" to "UP")
}
```

### Flyway Baseline Migration
```sql
-- Source: PostgreSQL + Flyway conventions
-- V1__phase01_baseline.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

### .gitignore for Monorepo
```gitignore
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
*.iml
.vscode/
*.swp

# Kotlin
*.class

# OS
.DS_Store
Thumbs.db

# Environment
.env
*.local

# Node (frontend)
frontend/node_modules/
frontend/.next/
frontend/out/

# Docker
docker-compose.override.yml
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| spring-boot-starter-web | spring-boot-starter-webmvc | Spring Boot 4.0 (Nov 2025) | Renamed starter; existing build already uses new name |
| com.fasterxml.jackson | tools.jackson | Spring Boot 4.0 (Jackson 3) | New Maven group ID; existing build already uses new group |
| flyway-core alone | spring-boot-starter-flyway | Spring Boot 4.0 modularization | Dedicated starter required for auto-configuration |
| Gradle subprojects {} block | plugins apply false + per-project apply | Gradle 7+ best practice | Cleaner dependency isolation, no cross-project leaking |
| application.properties flat | application.yml hierarchical | Convention, not version-gated | Better readability for nested Spring config |
| pgcrypto for gen_random_uuid() | Built-in since PostgreSQL 13 | PostgreSQL 13 (2020) | Extension optional but harmless to include |

**Deprecated/outdated:**
- `spring-boot-starter-web`: Renamed to `spring-boot-starter-webmvc` in SB4
- `com.fasterxml.jackson.*`: Jackson 3 uses `tools.jackson.*` group
- Lombok: Entirely unnecessary in Kotlin -- use data classes, named params, default values
- `subprojects {}` in root build.gradle.kts: Gradle discourages; use `apply false` pattern

## Open Questions

1. **Spring Boot 4.0.4 + Kotlin 2.2.21 + Java 24 compatibility**
   - What we know: Spring Boot 4 requires Java 17-25 and Kotlin 2.2+. Current config uses Java 24 toolchain.
   - What's unclear: Whether 4.0.4 specifically has been tested with Kotlin 2.2.21 (very recent Kotlin version).
   - Recommendation: Proceed as-is since the project was generated with these versions. If build issues arise, this is the first thing to check.

2. **IDE working directory for Docker Compose discovery**
   - What we know: Running via `./gradlew :backend:bootRun` from root works. IDE may set working directory to `/backend`.
   - What's unclear: IntelliJ's exact behavior with Gradle multi-project.
   - Recommendation: Add a note about `spring.docker.compose.file` property as a fallback if IDE-based runs fail.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 (Jupiter) + Spring Boot Test 4.0.4 |
| Config file | backend/build.gradle.kts (tasks.withType<Test> { useJUnitPlatform() }) |
| Quick run command | `./gradlew :backend:test` |
| Full suite command | `./gradlew :backend:test` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| INFR-01 | Docker Compose starts services | smoke | `docker compose up -d && docker compose ps` | N/A (shell command) |
| INFR-02 | PostgreSQL accessible in container | smoke | `docker compose exec postgres pg_isready` | N/A (shell command) |
| INFR-03 | Monorepo directories exist with builds | smoke | `./gradlew :backend:classes` | N/A (build verification) |
| INFR-04 | Flyway runs baseline migration | integration | `./gradlew :backend:test --tests "*ApplicationTests*"` | Yes (needs update) |
| DEVX-01 | CLAUDE.md files exist per module | manual-only | Verify file existence | N/A |
| DEVX-02 | Claude Code skills directory exists | manual-only | Verify directory structure | N/A |

### Sampling Rate
- **Per task commit:** `./gradlew :backend:test`
- **Per wave merge:** `./gradlew :backend:test` + `docker compose up -d && docker compose ps`
- **Phase gate:** Full build + Docker Compose up + Flyway migration verified

### Wave 0 Gaps
- [ ] `backend/src/test/kotlin/.../JobHuntApplicationTests.kt` -- move from root src/test, update to verify Flyway migration runs (context loads = migration executes)
- [ ] `backend/build.gradle.kts` -- test dependencies (spring-boot-starter-test, kotlin-test-junit5)
- [ ] Docker Compose must be running for @SpringBootTest to work (docker-compose starter handles this)

## Sources

### Primary (HIGH confidence)
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) - Renamed starters, modularization, Java/Kotlin requirements
- [Spring Boot Docker Compose Docs](https://docs.spring.io/spring-boot/how-to/docker-compose.html) - Auto-discovery, lifecycle management, JDBC auto-configuration
- [Gradle Multi-Project Builds](https://docs.gradle.org/current/userguide/multi_project_builds.html) - Multi-project structure, plugins apply false pattern
- Existing project files (build.gradle.kts, compose.yaml, settings.gradle.kts) - Current state of the codebase

### Secondary (MEDIUM confidence)
- [Spring Boot 4.0.0 Release Announcement](https://spring.io/blog/2025/11/20/spring-boot-4-0-0-available-now/) - Release date and highlights
- [Baeldung: Docker Compose Support in Spring Boot](https://www.baeldung.com/docker-compose-support-spring-boot) - Docker Compose integration patterns
- [JetBrains: Flyway Migrations in Spring Boot](https://blog.jetbrains.com/idea/2024/11/how-to-use-flyway-for-database-migrations-in-spring-boot-applications/) - Flyway setup patterns
- [PostgreSQL pgcrypto docs](https://www.postgresql.org/docs/current/pgcrypto.html) - Extension documentation, gen_random_uuid() history

### Tertiary (LOW confidence)
- [Flyway Migrations in Spring Boot 4.x: What Changed](https://pranavkhodanpur.medium.com/flyway-migrations-in-spring-boot-4-x-what-changed-and-how-to-configure-it-correctly-dbe290fa4d47) - SB4-specific Flyway changes (could not fetch full content)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Verified against existing build.gradle.kts and official Spring Boot 4.0 migration guide
- Architecture: HIGH - Gradle multi-project is well-documented; patterns cross-verified with multiple sources
- Pitfalls: HIGH - Based on known SB4 modularization changes and Gradle multi-project gotchas
- Docker Compose: HIGH - Official Spring Boot docs confirm auto-discovery and JDBC auto-configuration

**Research date:** 2026-03-19
**Valid until:** 2026-04-19 (stable stack, no fast-moving dependencies)
