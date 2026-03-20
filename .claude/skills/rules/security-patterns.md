# Security Patterns

## Authentication Architecture

1. JWT-based stateless authentication
2. Access token (short-lived, ~15 min) + Refresh token (longer-lived, ~7 days)
3. Tokens issued on login, validated on every request via filter
4. No server-side session storage — JWT is self-contained
5. Refresh tokens stored in database for revocation support

## Spring Security Configuration

1. Use `SecurityFilterChain` bean — never extend `WebSecurityConfigurerAdapter`
2. Configure via `HttpSecurity` DSL in a `@Configuration` class
3. Disable CSRF for stateless JWT API (no cookies)
4. Enable CORS with explicit allowed origins
5. Filter chain ordering: CORS -> JWT filter -> authorization

## Filter Chain Pattern

```kotlin
@Bean
fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
    http {
        csrf { disable() }
        cors { configurationSource = corsConfig() }
        sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
        authorizeHttpRequests {
            authorize("/api/auth/**", permitAll)
            authorize("/actuator/health", permitAll)
            authorize(anyRequest, authenticated)
        }
        addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthFilter)
    }
    return http.build()
}
```

## JWT Implementation

1. Use a well-maintained JWT library (e.g., jjwt or nimbus-jose-jwt)
2. Sign tokens with a strong secret (HS256 minimum, RS256 preferred for production)
3. Store signing key in environment variable or Spring config — never in code
4. Token payload: `sub` (user ID as UUID), `iat`, `exp` — keep claims minimal
5. Validate: signature, expiration, and issuer on every request

## Password Handling

1. Hash with BCrypt via Spring Security's `PasswordEncoder`
2. Never store plaintext passwords
3. Use `BCryptPasswordEncoder` with default strength (10 rounds)
4. Password requirements: minimum 8 characters (enforce in validation layer)

## User Entity

1. User table created via Flyway migration
2. Fields: id (UUID), email (unique), password_hash, created_at, updated_at
3. Email is the login identifier — case-insensitive matching
4. User entity never exposed directly in API responses — use UserResponse DTO

## Authorization Rules

1. Users can only access their own data — enforce at service layer
2. Every query filters by `userId` from JWT — never trust path/body user IDs
3. Return 404 (not 403) when accessing another user's resource — don't leak existence
4. Actuator health endpoint is public; all other actuator endpoints require auth

## CORS Configuration

1. Explicit allowed origins — never `*` in production
2. Allow: GET, POST, PUT, PATCH, DELETE, OPTIONS
3. Allow headers: Authorization, Content-Type
4. Expose headers: Location (for 201 responses)
5. OPTIONS preflight must pass without authentication

## Auth Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/auth/register` | POST | Public | Create account |
| `/api/auth/login` | POST | Public | Get JWT tokens |
| `/api/auth/refresh` | POST | Public | Refresh access token |
| `/api/auth/logout` | POST | Authenticated | Invalidate refresh token |
| `/api/auth/password-reset` | POST | Public | Request reset email |
| `/api/auth/password-reset/confirm` | POST | Public | Complete reset |

## Security Testing

1. Test every endpoint for 401 without token
2. Test every endpoint for 403 / 404 with wrong user's token
3. Test token expiration handling
4. Test malformed token rejection
5. Test CORS preflight succeeds for allowed origins
6. Use `@WithMockUser` for non-auth-specific tests

## Secure Actuator Access (Phase 2+)

1. `/actuator/health` stays public (load balancer health checks)
2. All other actuator endpoints require authentication
3. Consider a separate actuator security filter chain with stricter rules
