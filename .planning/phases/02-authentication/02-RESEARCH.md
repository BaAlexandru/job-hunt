# Phase 2: Authentication - Research

**Researched:** 2026-03-20
**Domain:** Spring Security 7 + JWT Authentication (Kotlin / Spring Boot 4)
**Confidence:** HIGH

## Summary

Phase 2 implements a complete authentication system using Spring Security 7 (bundled with Spring Boot 4.0.4) with custom JWT token management via the JJWT library. The architecture uses an access+refresh token pair with database-backed token blocklisting for invalidation, BCrypt password hashing, Flyway-managed schema for users/roles/tokens tables, and Redis for rate limiting password reset requests.

Spring Boot 4 ships with Spring Security 7, which has several breaking changes from 6.x: Jackson 3 instead of Jackson 2 for security serialization, PathPatternRequestMatcher as the default (replacing Ant/MVC matchers), and removal of the `and()` chaining method in favor of lambda DSL. The Kotlin DSL for Spring Security remains available via the `org.springframework.security.config.annotation.web.invoke` import and uses the `http { }` block syntax. JJWT 0.12.6 is the stable production choice (0.13.0 exists but targets Java 7 compatibility; no meaningful API changes from 0.12.x).

**Primary recommendation:** Use JJWT 0.12.6 for token creation/validation (not Spring OAuth2 Resource Server, which assumes an external authorization server), Spring Security 7 Kotlin DSL for filter chain configuration, BCrypt via `PasswordEncoderFactories.createDelegatingPasswordEncoder()` for password hashing, and Spring Data Redis for rate limiting.

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions
- Access + Refresh token pair (15min access / 7 day refresh)
- Access token in Authorization header, refresh token in HTTP-only cookie
- Refresh endpoint rotates both tokens
- Token invalidation via database blocklist table (store invalidated token IDs, check on each request)
- Logout adds both tokens to blocklist
- Password reset: log link to console in local dev, 1 hour single-use token, rate limited via Redis (3/email/hour)
- Redis added to Docker Compose stack for rate limiting
- Registration: min 8 chars, 1 uppercase, 1 lowercase, 1 number; generic error on duplicate email
- Email verification required: token logged to console, account locked until confirmed
- Public endpoints: /api/auth/** and /actuator/**; all others require valid JWT
- CORS: allow http://localhost:3000 only; OPTIONS requests pass without auth
- RBAC: USER and ADMIN roles; default USER on registration; @PreAuthorize for role checks

### Claude's Discretion
- BCrypt rounds/configuration for password hashing
- Exact JWT claims structure and signing algorithm
- Database blocklist cleanup strategy (scheduled job vs lazy cleanup)
- Redis configuration details and Spring Data Redis setup
- Email verification token format and URL structure
- Spring Security filter chain configuration details
- Exception handling and error response format for auth endpoints

### Deferred Ideas (OUT OF SCOPE)
- Auth.js vs custom JWT handling for frontend -- Phase 7 decision
- OAuth2 social login (Google, GitHub) -- future enhancement, not v1
- Account deletion/deactivation -- not in current requirements
- Two-factor authentication (2FA) -- potential v2 feature

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| AUTH-01 | User can create account with email and password | Spring Security UserDetailsService + BCrypt password encoding + Flyway users table migration + email verification flow |
| AUTH-02 | User can log in and stay logged in across sessions via JWT | JJWT token creation with access+refresh pair, Spring Security filter chain with custom JWT filter, refresh token in HTTP-only cookie |
| AUTH-03 | User can log out from any page | Token blocklist table in PostgreSQL, logout endpoint adds both tokens to blocklist, JWT filter checks blocklist on each request |
| AUTH-04 | User can reset password via email link | Password reset token table, console-logged reset URL, single-use 1hr tokens, Redis rate limiting (3/email/hour) |

</phase_requirements>

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| spring-boot-starter-security | 4.0.4 (managed) | Spring Security 7 integration | Official Spring Boot security starter; provides filter chain, auth managers, password encoders |
| io.jsonwebtoken:jjwt-api | 0.12.6 | JWT creation and validation API | De facto standard for custom JWT in Spring; fluent builder API, actively maintained |
| io.jsonwebtoken:jjwt-impl | 0.12.6 | JJWT implementation (runtime) | Required runtime dependency for jjwt-api |
| io.jsonwebtoken:jjwt-jackson | 0.12.6 | JJWT Jackson serialization (runtime) | JSON processing for JWT claims; aligns with project's Jackson usage |
| spring-boot-starter-data-redis | 4.0.4 (managed) | Redis client for rate limiting | Official Spring Boot Redis starter; provides RedisTemplate and connection auto-config |
| spring-boot-starter-validation | 4.0.4 (managed) | Bean Validation (jakarta.validation) | Input validation for registration/login DTOs (@NotBlank, @Email, @Size, @Pattern) |

### Testing

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| io.mockk:mockk | 1.14.2+ | Kotlin-native mocking | Unit tests for services; handles final classes, Kotlin idioms |
| com.ninja-squad:springmockk | 4.0.2+ | Spring Boot MockK integration | @WebMvcTest controller tests; replaces @MockBean with MockK equivalents |
| org.springframework.security:spring-security-test | 7.0.x (managed) | Security test utilities | Testing secured endpoints; @WithMockUser, SecurityMockMvcRequestPostProcessors |
| org.testcontainers:testcontainers | 1.20.x | Container-based testing | Integration tests needing real PostgreSQL/Redis |
| org.testcontainers:postgresql | 1.20.x | PostgreSQL test container | Integration tests with real DB |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| JJWT (custom JWT) | spring-boot-starter-security-oauth2-resource-server | OAuth2 RS assumes external auth server with JWKS endpoint; custom JWT gives full control over token lifecycle (blocklist, refresh rotation) which this project needs |
| Redis (rate limiting) | Bucket4j + Redis | Adds another library; simple INCR+EXPIRE in Redis is sufficient for 3/email/hour |
| Custom JWT filter | Spring OAuth2 BearerTokenAuthenticationFilter | Less control over blocklist checking, refresh flow, custom claims extraction |

**Installation (add to backend/build.gradle.kts dependencies block):**
```kotlin
// Security
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("org.springframework.boot:spring-boot-starter-validation")
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

// Redis for rate limiting
implementation("org.springframework.boot:spring-boot-starter-data-redis")

// Testing
testImplementation("org.springframework.security:spring-security-test")
testImplementation("io.mockk:mockk:1.14.2")
testImplementation("com.ninja-squad:springmockk:4.0.2")
testImplementation("org.testcontainers:junit-jupiter")
testImplementation("org.testcontainers:postgresql")
```

## Architecture Patterns

### Recommended Package Structure
```
com.alex.job.hunt.jobhunt/
  /config/
    SecurityConfig.kt          # SecurityFilterChain beans, CORS, CSRF
    RedisConfig.kt             # RedisTemplate configuration
  /security/
    JwtAuthenticationFilter.kt # OncePerRequestFilter - extracts & validates JWT
    JwtTokenProvider.kt        # Token creation, validation, claims extraction
    UserDetailsServiceImpl.kt  # Loads user from DB for Spring Security
  /controller/
    AuthController.kt          # /api/auth/** endpoints
  /service/
    AuthService.kt             # Registration, login, logout, refresh logic
    PasswordResetService.kt    # Reset token generation, validation, rate limiting
    EmailVerificationService.kt # Verification token generation, confirmation
  /repository/
    UserRepository.kt          # JpaRepository<UserEntity, UUID>
    TokenBlocklistRepository.kt # JpaRepository for blocked tokens
    PasswordResetTokenRepository.kt
    EmailVerificationTokenRepository.kt
  /entity/
    UserEntity.kt              # JPA entity: users table
    TokenBlocklistEntry.kt     # JPA entity: token_blocklist table
    PasswordResetToken.kt      # JPA entity: password_reset_tokens table
    EmailVerificationToken.kt  # JPA entity: email_verification_tokens table
  /dto/
    AuthRequest.kt             # Login request (email, password)
    RegisterRequest.kt         # Registration request (email, password)
    AuthResponse.kt            # Token response (accessToken, expiresIn)
    PasswordResetRequest.kt    # Reset request/confirm DTOs
```

### Pattern 1: Spring Security Kotlin DSL Filter Chain

**What:** Configure SecurityFilterChain using Kotlin DSL with `http { }` block syntax
**When to use:** All security configuration
**Example:**
```kotlin
// Source: https://docs.spring.io/spring-security/reference/servlet/configuration/kotlin.html
import org.springframework.security.config.annotation.web.invoke

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }  // Stateless JWT API, no CSRF needed
            cors { }  // Uses CorsConfigurationSource bean
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeHttpRequests {
                authorize("/api/auth/**", permitAll)
                authorize("/actuator/**", permitAll)
                authorize(anyRequest, authenticated)
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = listOf("http://localhost:3000")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager
}
```

### Pattern 2: Custom JWT Filter (OncePerRequestFilter)

**What:** Filter that extracts JWT from Authorization header, validates it, checks blocklist, sets SecurityContext
**When to use:** Every authenticated request
**Example:**
```kotlin
// Source: https://www.baeldung.com/kotlin/spring-security-jwt
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsService,
    private val tokenBlocklistRepository: TokenBlocklistRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)
        if (token != null && jwtTokenProvider.validateToken(token)) {
            val tokenId = jwtTokenProvider.getTokenId(token)
            if (!tokenBlocklistRepository.existsByTokenId(tokenId)) {
                val username = jwtTokenProvider.getUsername(token)
                val userDetails = userDetailsService.loadUserByUsername(username)
                val auth = UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.authorities
                )
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)
}
```

### Pattern 3: JJWT Token Creation/Validation

**What:** Service for creating and parsing JWT tokens using JJWT 0.12.x API
**When to use:** Login, refresh, and token validation
**Example:**
```kotlin
// Source: https://github.com/jwtk/jjwt
@Service
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-expiration-ms}") private val accessExpirationMs: Long,
    @Value("\${jwt.refresh-expiration-ms}") private val refreshExpirationMs: Long
) {
    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
    }

    fun createAccessToken(userId: UUID, email: String, role: String): String {
        val now = Date()
        return Jwts.builder()
            .id(UUID.randomUUID().toString())  // jti for blocklist
            .subject(email)
            .claim("userId", userId.toString())
            .claim("role", role)
            .issuedAt(now)
            .expiration(Date(now.time + accessExpirationMs))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean = try {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    } catch (e: JwtException) {
        false
    }

    fun getUsername(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject

    fun getTokenId(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.id
}
```

### Pattern 4: Redis Rate Limiting

**What:** Simple Redis INCR + EXPIRE for rate limiting password reset requests
**When to use:** Password reset endpoint -- max 3 per email per hour
**Example:**
```kotlin
@Service
class RateLimiter(private val redisTemplate: StringRedisTemplate) {

    fun isAllowed(key: String, maxRequests: Int, windowSeconds: Long): Boolean {
        val ops = redisTemplate.opsForValue()
        val current = ops.increment(key) ?: 1
        if (current == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
        }
        return current <= maxRequests
    }
}

// Usage: rateLimiter.isAllowed("reset:$email", 3, 3600)
```

### Anti-Patterns to Avoid
- **Storing JWT secret in code or application.yml directly:** Use environment variable or Spring config with `@Value` referencing a property that can be overridden. For local dev, a default in application.yml is acceptable but must be clearly marked.
- **Checking blocklist in the token provider:** Separation of concerns -- token provider creates/parses tokens; the filter is responsible for blocklist checking.
- **Using `WebSecurityConfigurerAdapter`:** Removed in Spring Security 6+. Use `SecurityFilterChain` bean instead.
- **Using `antMatchers()` or `mvcMatchers()`:** Removed in Spring Security 7. Use `requestMatchers()` or the Kotlin DSL `authorize()` syntax.
- **Storing raw passwords:** Always use `PasswordEncoder`; never store plaintext.
- **Returning specific errors for "email not found" vs "wrong password":** Enables user enumeration attacks. Return generic "Invalid credentials" for both.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Password hashing | Custom hash function | `PasswordEncoderFactories.createDelegatingPasswordEncoder()` | BCrypt with delegating encoder supports future algorithm upgrades; handles salt automatically |
| CORS handling | Manual filter for OPTIONS | Spring Security `cors { }` with `CorsConfigurationSource` bean | Properly handles preflight, integrates with filter chain ordering |
| Token signing | Manual HMAC implementation | JJWT `Jwts.builder().signWith(key)` | Handles algorithm selection, key validation, Base64 encoding |
| Input validation | Manual if/else checks | Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Size`, `@Pattern`) | Declarative, consistent error messages, integrates with Spring exception handling |
| Rate limiting atomic ops | Custom DB counter | Redis `INCR` + `EXPIRE` (atomic) | Race-condition-free, automatic expiry, no cleanup needed |
| Security context propagation | ThreadLocal management | Spring Security `SecurityContextHolder` | Handles async, thread pool, and servlet lifecycle correctly |

**Key insight:** Spring Security provides most of the authentication infrastructure. The custom parts are limited to: JWT token provider (JJWT), JWT filter (bridges JJWT to Spring Security), token blocklist (simple JPA), and rate limiting (simple Redis). Everything else (password encoding, CORS, authorization, method security) uses Spring's built-in components.

## Common Pitfalls

### Pitfall 1: Spring Security Auto-Secures Everything
**What goes wrong:** Adding `spring-boot-starter-security` immediately secures ALL endpoints including actuator. Application appears broken with 401 on every request.
**Why it happens:** Spring Security's default configuration requires authentication for all endpoints.
**How to avoid:** Define `SecurityFilterChain` bean immediately when adding the security starter. Explicitly permit `/api/auth/**` and `/actuator/**`.
**Warning signs:** 401 responses on actuator health checks, Swagger/Postman requests failing.

### Pitfall 2: CORS Filter Ordering with JWT
**What goes wrong:** OPTIONS preflight requests hit the JWT filter and get 401 because they have no Authorization header.
**Why it happens:** JWT filter runs before CORS headers are added, or CORS filter is not early enough in the chain.
**How to avoid:** Use Spring Security's built-in `cors { }` DSL which registers the CORS filter at the correct position (before security filters). Do NOT create a separate `@Bean CorsFilter` -- let Spring Security manage it via `CorsConfigurationSource`.
**Warning signs:** Browser shows CORS errors even though the API works from Postman.

### Pitfall 3: Kotlin DSL Import Missing
**What goes wrong:** `http { }` block does not compile; IDE shows unresolved reference.
**Why it happens:** The Kotlin DSL operator function requires a specific import.
**How to avoid:** Always add `import org.springframework.security.config.annotation.web.invoke` at the top of security config files.
**Warning signs:** Compilation error on `http { ... }` block.

### Pitfall 4: JPA Entity Must Match Flyway Schema Exactly
**What goes wrong:** Application fails to start with Hibernate validation error.
**Why it happens:** `hibernate.ddl-auto=validate` compares entities against the actual schema. Any mismatch (column name, type, nullable) causes startup failure.
**How to avoid:** Write Flyway migration first, then create entity to match exactly. Use `@Column(name = "...")` for clarity. Test by running the application after each migration+entity pair.
**Warning signs:** `SchemaManagementException` at startup.

### Pitfall 5: Refresh Token in Cookie Not Sent Cross-Origin
**What goes wrong:** Frontend at localhost:3000 never receives the refresh token cookie from API at localhost:8080.
**Why it happens:** Cookies require `SameSite=None; Secure` for cross-origin, or `SameSite=Lax` with same-site. In local dev, ports differ making them cross-origin.
**How to avoid:** Set cookie with `SameSite=Lax` and `Path=/api/auth/refresh`. For local dev, ensure CORS `allowCredentials = true` and frontend sends `credentials: 'include'`. The cookie will work because both are on localhost despite different ports (browsers treat same host + different port as same-site for cookies).
**Warning signs:** Cookie not present in subsequent requests from browser.

### Pitfall 6: Spring Boot 4 / Spring Security 7 Breaking Changes
**What goes wrong:** Code from Spring Security 6 tutorials doesn't compile.
**Why it happens:** Spring Security 7 removed `antMatchers()`/`mvcMatchers()`, uses Jackson 3, requires PathPatternRequestMatcher.
**How to avoid:** Use the Kotlin DSL `authorize()` function which abstracts path matching. Do not copy code from pre-2025 tutorials without adapting. Use `requestMatchers()` in Java-style config.
**Warning signs:** Compilation errors mentioning `antMatchers`, `MvcRequestMatcher`, `ObjectMapper` (should be Jackson 3 `JsonMapper`).

### Pitfall 7: @MockBean Deprecated in Spring Boot 4
**What goes wrong:** Tests using `@MockBean` produce deprecation warnings or don't work.
**Why it happens:** Spring Boot 4 deprecates `@MockBean` and `@SpyBean` in favor of `@MockitoBean` and `@MockitoSpyBean`.
**How to avoid:** Use SpringMockK's `@MockkBean` for Kotlin tests, or the new `@MockitoBean` if using Mockito.
**Warning signs:** Deprecation warnings in test output.

## Code Examples

### Flyway Migration: Users Table
```sql
-- V2__phase02_create_users.sql
CREATE TABLE users (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL DEFAULT 'USER',
    enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);
```

### Flyway Migration: Token Blocklist
```sql
-- V3__phase02_token_blocklist.sql
CREATE TABLE token_blocklist (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id   VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_token_blocklist_token_id ON token_blocklist(token_id);
CREATE INDEX idx_token_blocklist_expires_at ON token_blocklist(expires_at);
```

### Flyway Migration: Verification & Reset Tokens
```sql
-- V4__phase02_verification_reset_tokens.sql
CREATE TABLE email_verification_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

### UserEntity JPA Mapping
```kotlin
@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var role: Role = Role.USER,

    @Column(nullable = false)
    var enabled: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)

enum class Role {
    USER, ADMIN
}
```

### Registration DTO with Bean Validation
```kotlin
data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one number"
    )
    val password: String
)
```

### application.yml JWT Configuration
```yaml
jwt:
  secret: ${JWT_SECRET:dGhpcy1pcy1hLWxvY2FsLWRldi1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2}
  access-expiration-ms: 900000     # 15 minutes
  refresh-expiration-ms: 604800000 # 7 days
```

### Docker Compose with Redis
```yaml
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

  redis:
    image: 'redis:7-alpine'
    ports:
      - '6379:6379'

volumes:
  pgdata:
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `WebSecurityConfigurerAdapter` | `SecurityFilterChain` @Bean | Spring Security 5.7 / removed in 6.0 | All config must use component-based approach |
| `antMatchers()` / `mvcMatchers()` | `requestMatchers()` / Kotlin DSL `authorize()` | Spring Security 6.0 / removed in 7.0 | Path matching uses PathPatternRequestMatcher |
| `@MockBean` / `@SpyBean` | `@MockitoBean` / `@MockitoSpyBean` (or SpringMockK `@MockkBean`) | Spring Boot 4.0 | Test annotations changed |
| Jackson 2 (`ObjectMapper`) | Jackson 3 (`JsonMapper`) | Spring Boot 4.0 / Spring Security 7.0 | Security serialization uses `tools.jackson` packages |
| `and()` chaining in HttpSecurity | Lambda DSL (Kotlin: `http { }` block) | Spring Security 6.0 / removed in 7.0 | Cannot chain with `.and()` anymore |
| JJWT `setSubject()` | JJWT `.subject()` | JJWT 0.12.0 | Builder methods simplified (both still work) |

**Deprecated/outdated:**
- `WebSecurityConfigurerAdapter`: Completely removed. Use `SecurityFilterChain` bean.
- `@MockBean`/`@SpyBean`: Deprecated in Spring Boot 4. Use SpringMockK or `@MockitoBean`.
- AntPathRequestMatcher: Replaced by PathPatternRequestMatcher in Spring Security 7.

## Open Questions

1. **SpringMockK compatibility with Spring Boot 4**
   - What we know: SpringMockK 4.x works with Spring Boot 3.x. Spring Boot 4 deprecates @MockBean in favor of @MockitoBean.
   - What's unclear: Whether SpringMockK has released a version compatible with Spring Boot 4's new test infrastructure.
   - Recommendation: Try SpringMockK 4.0.2 first. If incompatible, fall back to MockK directly with manual context configuration, or use @MockitoBean with Mockito (less idiomatic Kotlin).

2. **JJWT compatibility with Jackson 3 (Spring Boot 4)**
   - What we know: JJWT 0.12.6 uses `jjwt-jackson` which depends on Jackson 2 (`com.fasterxml.jackson`). Spring Boot 4 uses Jackson 3 (`tools.jackson`).
   - What's unclear: Whether Spring Boot 4's dependency management handles the Jackson 2/3 coexistence, or if jjwt-jackson conflicts.
   - Recommendation: Test during Wave 0 setup. If conflicts arise, JJWT can use `jjwt-gson` instead of `jjwt-jackson`, or manually manage Jackson versions. Spring Boot 4 may still include Jackson 2 compatibility bridge.

3. **Blocklist cleanup strategy**
   - What we know: Expired tokens in the blocklist serve no purpose after their expiry time passes.
   - What's unclear: Optimal cleanup frequency; whether a scheduled job or lazy deletion performs better.
   - Recommendation: Use a `@Scheduled` task that runs daily to delete entries where `expires_at < now()`. Simple, predictable, low overhead. Can be refined later.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + MockK 1.14.x + SpringMockK 4.0.x |
| Config file | backend/build.gradle.kts (testImplementation dependencies) |
| Quick run command | `./gradlew :backend:test --tests "*.auth.*"` |
| Full suite command | `./gradlew :backend:test` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | Register with email+password, receive verification email, verify, receive JWT | integration | `./gradlew :backend:test --tests "*AuthControllerTests*register*"` | No - Wave 0 |
| AUTH-01 | Reject duplicate email with generic error | integration | `./gradlew :backend:test --tests "*AuthControllerTests*duplicateEmail*"` | No - Wave 0 |
| AUTH-01 | Reject weak password | unit | `./gradlew :backend:test --tests "*AuthServiceTests*weakPassword*"` | No - Wave 0 |
| AUTH-02 | Login returns access + refresh tokens | integration | `./gradlew :backend:test --tests "*AuthControllerTests*login*"` | No - Wave 0 |
| AUTH-02 | Refresh endpoint rotates both tokens | integration | `./gradlew :backend:test --tests "*AuthControllerTests*refresh*"` | No - Wave 0 |
| AUTH-02 | JWT validates and sets security context | unit | `./gradlew :backend:test --tests "*JwtAuthenticationFilterTests*"` | No - Wave 0 |
| AUTH-03 | Logout blocklists tokens, subsequent requests rejected | integration | `./gradlew :backend:test --tests "*AuthControllerTests*logout*"` | No - Wave 0 |
| AUTH-04 | Password reset flow: request, receive token, reset | integration | `./gradlew :backend:test --tests "*PasswordResetTests*"` | No - Wave 0 |
| AUTH-04 | Rate limiting: 4th reset request rejected | integration | `./gradlew :backend:test --tests "*PasswordResetTests*rateLimit*"` | No - Wave 0 |
| - | CORS allows localhost:3000, blocks others | integration | `./gradlew :backend:test --tests "*CorsTests*"` | No - Wave 0 |
| - | Protected endpoints return 401 without JWT | integration | `./gradlew :backend:test --tests "*SecurityConfigTests*"` | No - Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :backend:test --tests "*.auth.*" -x check`
- **Per wave merge:** `./gradlew :backend:test`
- **Phase gate:** Full suite green before verification

### Wave 0 Gaps
- [ ] Add test dependencies to `backend/build.gradle.kts` (MockK, SpringMockK, spring-security-test, Testcontainers)
- [ ] Create `backend/src/test/kotlin/com/alex/job/hunt/jobhunt/auth/` test package
- [ ] Verify Docker Compose test connectivity works with Redis (existing `skip.in-tests: false` config)

## Sources

### Primary (HIGH confidence)
- [Spring Security Kotlin DSL docs](https://docs.spring.io/spring-security/reference/servlet/configuration/kotlin.html) - SecurityFilterChain configuration, import requirements, DSL syntax
- [Spring Security JWT Resource Server docs](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html) - JWT configuration patterns (evaluated and rejected in favor of JJWT for this use case)
- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) - Starter renames, Spring Security 7, @MockBean deprecation, Jackson 3
- [JJWT GitHub](https://github.com/jwtk/jjwt) - API reference, version 0.12.6 confirmed stable
- [BCryptPasswordEncoder API (Spring Security 7.0.3)](https://docs.spring.io/spring-security/reference/api/java/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html) - Password encoding

### Secondary (MEDIUM confidence)
- [Baeldung: Spring Security JWT with Kotlin](https://www.baeldung.com/kotlin/spring-security-jwt) - JWT filter pattern, verified against official docs
- [Spring Security 7 Migration](https://docs.spring.io/spring-security/reference/migration/index.html) - Breaking changes overview
- [Redis rate limiting tutorial](https://redis.io/tutorials/rate-limiting-in-java-spring-with-redis/) - INCR+EXPIRE pattern

### Tertiary (LOW confidence)
- SpringMockK 4.x compatibility with Spring Boot 4 - needs runtime validation
- JJWT Jackson 3 compatibility - needs build-time validation
- Testcontainers with Spring Boot 4 PostgreSQL - [known issues reported](https://github.com/spring-projects/spring-boot/issues/48234)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Spring Security + JJWT is the de facto pattern; versions confirmed via Maven Central and official docs
- Architecture: HIGH - Filter chain + custom JWT provider is the standard pattern for custom token management in Spring Security
- Pitfalls: HIGH - Well-documented migration issues from Spring Security 6 to 7; CORS ordering is a perennial issue
- Testing: MEDIUM - MockK/SpringMockK compatibility with Spring Boot 4 needs validation
- Redis integration: HIGH - Spring Data Redis with simple INCR+EXPIRE is a well-established pattern

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable stack, Spring Boot 4.0.4 is current)
