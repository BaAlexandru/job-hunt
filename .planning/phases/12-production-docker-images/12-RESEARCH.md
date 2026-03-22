# Phase 12: Production Docker Images - Research

**Researched:** 2026-03-22
**Domain:** Docker multi-stage builds for Spring Boot (Kotlin/JDK 24) and Next.js 16 (Node 22)
**Confidence:** HIGH

## Summary

Phase 12 produces two optimized Docker images: a Spring Boot backend using Eclipse Temurin JRE 24 Alpine and a Next.js 16 frontend using Node.js 22 Alpine standalone output. Both must be under 200MB and serve traffic when run with `docker run`.

The backend uses Spring Boot's `jarmode=tools extract --layers` feature (introduced in Spring Boot 3.3+, replacing the older `jarmode=layertools`) to create a layered image. The builder stage compiles with Gradle inside Docker (monorepo root as build context), then extracts layers. The runtime stage copies only extracted layers onto a JRE-alpine base. With JRE 24 Alpine base (~90MB) plus application layers (~60-80MB), the 200MB target is achievable.

The frontend uses Next.js standalone output mode (`output: "standalone"` in next.config.ts) which traces dependencies and produces a self-contained directory. A three-stage Dockerfile (deps, builder, runner) copies only `.next/standalone`, `.next/static`, and `public/` into the runner stage. Node 22 Alpine slim images are ~50MB, and standalone output is typically 30-80MB, well under the 200MB target.

**Primary recommendation:** Use Spring Boot layer extraction with `java -Djarmode=tools` for backend and the official Next.js three-stage standalone Dockerfile pattern for frontend. Both images run as non-root users with HEALTHCHECK instructions.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Dockerfiles live in project roots: `backend/Dockerfile` and `frontend/Dockerfile`
- Build context is project root (monorepo) for backend: `docker build -f backend/Dockerfile .`
- Build context is `frontend/` for frontend: `docker build -f frontend/Dockerfile frontend/`
- Full build inside Docker (multi-stage) -- no pre-built artifacts
- Backend: COPY gradlew, gradle/, settings.gradle.kts, build.gradle.kts, backend/ into builder stage, run `./gradlew :backend:bootJar`
- Frontend: COPY package.json, pnpm-lock.yaml first for dependency caching, then source, run `pnpm build`
- Dependency layers cached separately from source for faster rebuilds
- Docker Compose for local production testing: `compose.prod.yaml` extends existing `compose.yaml`
- HEALTHCHECK instructions included in both Dockerfiles
- Phase 12 images run with existing `application.yml` dev config -- no environment-specific profiles
- Base images: Eclipse Temurin JDK 24 Alpine (builder) / JRE 24 Alpine (runtime) for backend; Node.js 22 Alpine for frontend
- Both containers run as non-root user (dedicated `app` user created in Dockerfile)
- JVM flags: `-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError`

### Claude's Discretion
- Exact .dockerignore patterns per module
- HEALTHCHECK implementation details (endpoint, interval, timeout)
- Exact compose.prod.yaml structure and service naming
- Spring Boot layer extraction strategy (layertools vs fat JAR)

### Deferred Ideas (OUT OF SCOPE)
- AWS secrets management integration -- Deferred to Phase 14
- Environment-specific profile YAMLs (`application-staging.yml`, `application-prod.yml`) -- Deferred to Phase 14
- AWS Secrets Manager vs SSM Parameter Store choice -- Deferred to Phase 14
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DOCK-01 | Backend produces a multi-stage Docker image (JRE-alpine, <200MB) | Spring Boot `jarmode=tools extract --layers` on Temurin JRE 24 Alpine; verified pattern from official Spring Boot docs |
| DOCK-02 | Frontend produces a multi-stage Docker image (Next.js standalone, <200MB) | Next.js `output: "standalone"` with three-stage Dockerfile; verified pattern from official Next.js docs and examples |
</phase_requirements>

## Standard Stack

### Core
| Component | Version/Tag | Purpose | Why Standard |
|-----------|-------------|---------|--------------|
| eclipse-temurin | 24-jdk-alpine | Backend builder base | Official Adoptium JDK; Alpine keeps image small |
| eclipse-temurin | 24-jre-alpine | Backend runtime base | True JRE image (~90MB); Corretto lacks JRE Alpine |
| node | 22-alpine | Frontend builder + runtime base | Active LTS until April 2027; Alpine ~50MB |
| Gradle wrapper | 9.3.1 | Backend build tool (inside Docker) | Matches project's existing wrapper version |
| pnpm | (via corepack) | Frontend package manager | Project uses pnpm; corepack enables it in Node images |

### Supporting
| Component | Purpose | When to Use |
|-----------|---------|-------------|
| Spring Boot jarmode=tools | Layer extraction for Docker caching | Always -- extracts dependencies/spring-boot-loader/snapshot-deps/application layers |
| Next.js standalone output | Minimal self-contained server | Always -- reduces image from ~2GB to ~100-150MB |
| compose.prod.yaml | Local production testing | Extends compose.yaml with backend+frontend services |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Layer extraction | Fat JAR copy | Fat JAR is simpler but no Docker layer caching -- every code change re-pulls ~80MB of dependencies |
| Alpine base images | Debian slim | Debian slim is ~80MB vs Alpine ~5MB; Alpine is sufficient for JRE and Node workloads |
| corepack for pnpm | npm install -g pnpm | corepack is built into Node 22 and manages versions automatically |

## Architecture Patterns

### Recommended Project Structure
```
backend/
  Dockerfile            # Multi-stage: JDK builder -> JRE runner
  .dockerignore         # Exclude build/, .gradle/, etc.
frontend/
  Dockerfile            # Multi-stage: deps -> builder -> runner
  .dockerignore         # Exclude .next/, node_modules/, etc.
compose.yaml            # Existing dev services (postgres, redis, minio)
compose.prod.yaml       # Extends compose.yaml, adds backend + frontend services
```

### Pattern 1: Spring Boot Layer Extraction (Backend)
**What:** Three-stage Dockerfile -- build JAR, extract layers, assemble runtime image
**When to use:** Always for Spring Boot production images

```dockerfile
# Source: https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html

# Stage 1: Build
FROM eclipse-temurin:24-jdk-alpine AS builder
WORKDIR /build
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/
COPY backend/ backend/
RUN chmod +x gradlew && ./gradlew :backend:bootJar --no-daemon

# Stage 2: Extract layers
FROM eclipse-temurin:24-jdk-alpine AS extractor
WORKDIR /extract
COPY --from=builder /build/backend/build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Stage 3: Runtime
FROM eclipse-temurin:24-jre-alpine AS runtime
WORKDIR /application
RUN addgroup -S app && adduser -S app -G app
COPY --from=extractor /extract/extracted/dependencies/ ./
COPY --from=extractor /extract/extracted/spring-boot-loader/ ./
COPY --from=extractor /extract/extracted/snapshot-dependencies/ ./
COPY --from=extractor /extract/extracted/application/ ./
RUN chown -R app:app /application
USER app
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseContainerSupport", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "application.jar"]
```

**Key insight:** The `jarmode=tools` command (Spring Boot 3.3+/4.x) replaces the deprecated `jarmode=layertools`. It extracts four layers: dependencies, spring-boot-loader, snapshot-dependencies, and application. Dependencies rarely change, so Docker caches them efficiently.

### Pattern 2: Next.js Standalone Output (Frontend)
**What:** Three-stage Dockerfile -- install deps, build, minimal runtime
**When to use:** Always for Next.js production images

```dockerfile
# Source: https://github.com/vercel/next.js/tree/canary/examples/with-docker

# Stage 1: Dependencies
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN corepack enable pnpm && pnpm install --frozen-lockfile

# Stage 2: Build
FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NODE_ENV=production
ARG NEXT_PUBLIC_API_URL=http://localhost:8080/api
ENV NEXT_PUBLIC_API_URL=${NEXT_PUBLIC_API_URL}
RUN corepack enable pnpm && pnpm build

# Stage 3: Runner
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder --chown=app:app /app/public ./public
RUN mkdir .next && chown app:app .next
COPY --from=builder --chown=app:app /app/.next/standalone ./
COPY --from=builder --chown=app:app /app/.next/static ./.next/static
USER app
EXPOSE 3000
CMD ["node", "server.js"]
```

**Key insight:** `output: "standalone"` in next.config.ts makes Next.js trace all imports and produce a self-contained `server.js` plus only the node_modules actually used at runtime. This eliminates the full node_modules directory from the final image.

### Pattern 3: compose.prod.yaml Override
**What:** Docker Compose override file that adds backend and frontend services alongside existing infrastructure
**When to use:** Local production testing

```yaml
# compose.prod.yaml -- extends compose.yaml
services:
  backend:
    build:
      context: .
      dockerfile: backend/Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/jobhunt
      SPRING_DATASOURCE_USERNAME: jobhunt
      SPRING_DATASOURCE_PASSWORD: jobhunt
      SPRING_DATA_REDIS_HOST: redis
    depends_on:
      postgres:
        condition: service_started
      redis:
        condition: service_started

  frontend:
    build:
      context: frontend
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    environment:
      NEXT_PUBLIC_API_URL: http://backend:8080/api
    depends_on:
      - backend
```

### Anti-Patterns to Avoid
- **Copying entire monorepo into frontend build:** Frontend build context should be `frontend/` only, not project root. It does not need Gradle files.
- **Using JDK as runtime base:** JDK images are 200-300MB larger than JRE. Always use `eclipse-temurin:24-jre-alpine` for runtime.
- **Running as root:** Both images must create and switch to a non-root `app` user.
- **Skipping .dockerignore:** Without it, `.git/`, `node_modules/`, `build/` directories bloat the build context and break layer caching.
- **Hardcoding secrets in Dockerfile:** No passwords or API keys in Dockerfiles -- use environment variables at runtime.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JAR layer extraction | Custom unzip/copy scripts | `java -Djarmode=tools -jar app.jar extract --layers` | Spring Boot knows its own layer boundaries; custom scripts miss loader classes |
| Next.js dependency tracing | Manual node_modules pruning | `output: "standalone"` in next.config.ts | Next.js traces actual imports; manual pruning misses transitive deps or includes unused ones |
| pnpm installation in Docker | `npm install -g pnpm` | `corepack enable pnpm` | corepack is built into Node 22, manages pnpm version from package.json's packageManager field |
| Health checking | Custom scripts | Docker HEALTHCHECK with curl/wget to actual endpoints | Standard Docker mechanism, visible in `docker ps` |

**Key insight:** Both Spring Boot and Next.js have built-in Docker optimization features. Using them correctly yields better results than manual optimization.

## Common Pitfalls

### Pitfall 1: Gradle Daemon in Docker
**What goes wrong:** Gradle daemon starts, consumes memory, and may cause OOM in constrained Docker builds.
**Why it happens:** Gradle daemon is enabled by default for interactive use.
**How to avoid:** Always pass `--no-daemon` to Gradle commands in Dockerfiles.
**Warning signs:** Build hangs or fails with memory errors during `docker build`.

### Pitfall 2: Missing gradlew Execute Permission
**What goes wrong:** `./gradlew` fails with "Permission denied" inside Docker.
**Why it happens:** Git on Windows may not preserve the execute bit on `gradlew`.
**How to avoid:** Add `RUN chmod +x gradlew` before running Gradle in the Dockerfile.
**Warning signs:** Build fails immediately at the Gradle step.

### Pitfall 3: Next.js standalone Missing public/ and static/
**What goes wrong:** Images, fonts, and static assets return 404.
**Why it happens:** `output: "standalone"` only traces JS dependencies. It does NOT include `public/` or `.next/static/` in the standalone directory.
**How to avoid:** Explicitly COPY both `public/` and `.next/static/` in the runner stage (as shown in the pattern above).
**Warning signs:** App loads but CSS/images/fonts are broken.

### Pitfall 4: Build Context Too Large
**What goes wrong:** `docker build` takes minutes just to send context; layer caching breaks on unrelated file changes.
**Why it happens:** Without `.dockerignore`, Docker sends `.git/`, `node_modules/`, `build/`, `.next/` etc.
**How to avoid:** Create `.dockerignore` files excluding build artifacts, VCS files, and IDE configs.
**Warning signs:** "Sending build context to Docker daemon" shows hundreds of MB.

### Pitfall 5: Spring Boot docker-compose Starter in Prod Image
**What goes wrong:** Spring Boot tries to manage Docker Compose at startup inside a container.
**Why it happens:** `spring-boot-docker-compose` dependency included in production build.
**How to avoid:** The project already has this as `testAndDevelopmentOnly` scope -- no action needed. Verify it does NOT appear in the bootJar.
**Warning signs:** Container logs show "Docker Compose lifecycle management" messages.

### Pitfall 6: next.config.ts Missing output: "standalone"
**What goes wrong:** `.next/standalone/` directory not generated; COPY in Dockerfile fails.
**Why it happens:** Forgetting to add the config before building.
**How to avoid:** Add `output: "standalone"` to next.config.ts as the first implementation step.
**Warning signs:** Build succeeds but `.next/standalone` directory does not exist.

### Pitfall 7: NEXT_PUBLIC_ Env Vars Must Be Set at Build Time
**What goes wrong:** Frontend shows `undefined` for API URL at runtime.
**Why it happens:** Next.js inlines `NEXT_PUBLIC_*` variables during `next build`. They cannot be changed at runtime.
**How to avoid:** Use `ARG NEXT_PUBLIC_API_URL` and pass as `--build-arg` during `docker build`. For Phase 12, the default `http://localhost:8080/api` is fine.
**Warning signs:** Browser console shows API calls to `undefined/api/...`.

### Pitfall 8: Alpine + glibc Dependencies
**What goes wrong:** Native Node.js modules or Java libraries fail on Alpine (which uses musl libc).
**Why it happens:** Some npm packages bundle glibc-compiled binaries.
**How to avoid:** This project uses pure-JS dependencies (no native modules like sharp or bcrypt with C bindings). Eclipse Temurin Alpine images include the necessary glibc compatibility layer. Monitor for errors during build.
**Warning signs:** `Error: Error loading shared library` during build or runtime.

## Code Examples

### next.config.ts Update
```typescript
// Add output: "standalone" for Docker deployment
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  allowedDevOrigins: ["192.168.178.49"],
};

export default nextConfig;
```

### Backend .dockerignore
```
# Build artifacts
backend/build/
backend/.gradle/
.gradle/

# IDE
.idea/
*.iml

# Git
.git/
.gitignore

# Frontend (not needed for backend build)
frontend/

# Planning docs
.planning/
.agents/
.claude/

# Docker files
**/Dockerfile
**/.dockerignore
*.md
```

### Frontend .dockerignore
```
# Build artifacts
.next/
node_modules/
out/

# IDE
.idea/
*.iml

# Git
.git/
.gitignore

# Tests
**/*.test.*
**/*.spec.*
vitest.config.*
coverage/

# Docker
Dockerfile
.dockerignore

# Misc
*.md
.env*
```

### HEALTHCHECK Instructions

Backend:
```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

Frontend:
```dockerfile
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3000/ || exit 1
```

**Note:** Use `wget` instead of `curl` -- Alpine images include wget by default but not curl. This avoids installing additional packages.

### compose.prod.yaml for Local Testing
```yaml
# Extends compose.yaml -- run with:
# docker compose -f compose.yaml -f compose.prod.yaml up --build
services:
  backend:
    build:
      context: .
      dockerfile: backend/Dockerfile
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/jobhunt
      SPRING_DATASOURCE_USERNAME: jobhunt
      SPRING_DATASOURCE_PASSWORD: jobhunt
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: "6379"
    depends_on:
      postgres:
        condition: service_started
      redis:
        condition: service_started
      minio:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      start_period: 60s
      retries: 3

  frontend:
    build:
      context: frontend
      dockerfile: Dockerfile
      args:
        NEXT_PUBLIC_API_URL: http://localhost:8080/api
    ports:
      - "3000:3000"
    depends_on:
      backend:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:3000/"]
      interval: 30s
      timeout: 5s
      start_period: 15s
      retries: 3
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `java -Djarmode=layertools extract` | `java -Djarmode=tools -jar app.jar extract --layers` | Spring Boot 3.3 (2024) | New command syntax; old one still works but deprecated |
| Fat JAR copy into image | Layer extraction with 4 separate COPY commands | Spring Boot 2.3 (2020) | Docker layer caching for unchanged dependencies |
| Full node_modules in Next.js image | `output: "standalone"` with traced dependencies | Next.js 12 (2021) | Image size from ~2GB to ~100-150MB |
| `npm install -g pnpm` | `corepack enable pnpm` | Node 16.13+ (2021) | Built-in pnpm management, no separate install |
| `eclipse-temurin:24-jre-alpine-3.21` | `eclipse-temurin:24-jre-alpine` (now defaults to 3.22) | 2025 | Alpine 3.22 is current default |

**Deprecated/outdated:**
- `jarmode=layertools`: Deprecated in Spring Boot 3.3+, replaced by `jarmode=tools`
- `NEXT_PUBLIC_` runtime override: Not possible -- these are always build-time inlined

## Open Questions

1. **MinIO S3 endpoint configuration in compose.prod.yaml**
   - What we know: Backend uses AWS SDK S3 client pointing to MinIO for local dev. The endpoint and credentials need to be passed as environment variables.
   - What's unclear: Exact Spring property names for S3 endpoint override (likely custom config, not standard Spring properties)
   - Recommendation: Check `application.yml` for MinIO/S3 config properties and replicate in compose.prod.yaml environment section

2. **Spring Boot AOT Cache (Java 24 feature)**
   - What we know: Spring Boot docs show AOT cache generation for Java 24+ that improves startup time significantly
   - What's unclear: Whether AOT cache works reliably with Spring Boot 4.0.4 + all project dependencies
   - Recommendation: Skip AOT cache for Phase 12 (adds complexity, not required by success criteria). Can be added as optimization later.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Manual Docker build + run validation |
| Config file | None -- Docker-based validation |
| Quick run command | `docker build -f backend/Dockerfile . && docker build -f frontend/Dockerfile frontend/` |
| Full suite command | `docker compose -f compose.yaml -f compose.prod.yaml up --build -d && sleep 10 && curl http://localhost:8080/actuator/health && curl http://localhost:3000` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DOCK-01 | Backend image builds, <200MB, serves traffic | smoke | `docker build -f backend/Dockerfile -t jobhunt-backend . && docker image inspect jobhunt-backend --format '{{.Size}}'` | N/A (Dockerfile) |
| DOCK-02 | Frontend image builds, <200MB, serves traffic | smoke | `docker build -f frontend/Dockerfile -t jobhunt-frontend frontend/ && docker image inspect jobhunt-frontend --format '{{.Size}}'` | N/A (Dockerfile) |

### Sampling Rate
- **Per task commit:** Build the relevant Docker image and check size
- **Per wave merge:** Full `docker compose -f compose.yaml -f compose.prod.yaml up --build` test
- **Phase gate:** Both images build, both under 200MB, both respond to health checks

### Wave 0 Gaps
- [ ] `frontend/next.config.ts` -- needs `output: "standalone"` added
- [ ] `backend/Dockerfile` -- does not exist yet
- [ ] `frontend/Dockerfile` -- does not exist yet
- [ ] `backend/.dockerignore` -- does not exist yet
- [ ] `frontend/.dockerignore` -- does not exist yet
- [ ] `compose.prod.yaml` -- does not exist yet
- [ ] `infra/CLAUDE.md` -- needs update to reflect module-root Dockerfile placement (partially done)

## Sources

### Primary (HIGH confidence)
- [Spring Boot Dockerfiles documentation](https://docs.spring.io/spring-boot/reference/packaging/container-images/dockerfiles.html) -- layer extraction with `jarmode=tools`, multi-stage Dockerfile pattern
- [Next.js Deploying documentation](https://nextjs.org/docs/app/getting-started/deploying) -- Docker deployment with standalone output, official examples
- [Next.js with-docker example](https://github.com/vercel/next.js/tree/canary/examples/with-docker) -- Official three-stage Dockerfile
- [Eclipse Temurin Docker Hub](https://hub.docker.com/_/eclipse-temurin) -- JDK/JRE Alpine image tags and availability

### Secondary (MEDIUM confidence)
- [Adoptium Temurin 24.0.2 release](https://adoptium.net/news/2025/07/eclipse-temurin-8u462-11028-17016-2108-2402-available/) -- Confirms Temurin 24 availability
- [Docker multi-stage builds documentation](https://docs.docker.com/get-started/docker-concepts/building-images/multi-stage-builds/) -- General multi-stage build patterns

### Tertiary (LOW confidence)
- None -- all findings verified with official sources

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all base images verified on Docker Hub, versions confirmed
- Architecture: HIGH -- patterns sourced from official Spring Boot and Next.js documentation
- Pitfalls: HIGH -- based on known Docker/Alpine/Gradle/Next.js behaviors, verified across multiple sources

**Research date:** 2026-03-22
**Valid until:** 2026-04-22 (30 days -- stable domain, base image tags are versioned)
