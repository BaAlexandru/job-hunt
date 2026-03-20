# REST API Patterns

## Endpoint Design

1. All domain endpoints under `/api/` prefix — no versioning
2. RESTful resource naming: plural nouns (`/api/companies`, `/api/jobs`, `/api/applications`)
3. Nested resources for strong ownership: `/api/applications/{id}/notes`
4. Use path parameters for identity, query parameters for filtering/sorting/paging
5. Actuator endpoints at `/actuator/` — never mix with domain endpoints

## Controllers

1. One controller per domain aggregate (e.g., `CompanyController`, `ApplicationController`)
2. Use `@RestController` + `@RequestMapping("/api/resource")`
3. Constructor injection for services — never `@Autowired` fields
4. Controllers are thin: validate input, delegate to service, map response
5. Extract authenticated user from `SecurityContext` — never trust client-sent user IDs

## Request/Response DTOs

1. Use Kotlin `data class` for all request and response bodies
2. Separate request and response DTOs — never expose entities directly
3. Suffix naming: `CreateCompanyRequest`, `CompanyResponse`, `UpdateJobRequest`
4. Keep DTOs in the same package as their controller or in a `dto` subpackage
5. Never include internal fields in responses (user_id, internal status codes)

## HTTP Status Codes

| Operation | Success | Common Errors |
|-----------|---------|---------------|
| GET one | 200 | 404 |
| GET list | 200 (empty list is 200, not 404) | — |
| POST create | 201 + Location header | 400, 409 (duplicate) |
| PUT update | 200 | 400, 404 |
| PATCH partial | 200 | 400, 404 |
| DELETE | 204 (no body) | 404 |

## Error Handling

1. Use `@RestControllerAdvice` for global exception handling
2. Standard error response shape:
   ```json
   {
     "status": 400,
     "error": "Bad Request",
     "message": "Company name is required",
     "path": "/api/companies",
     "timestamp": "2026-03-20T12:00:00Z"
   }
   ```
3. Map domain exceptions to HTTP status codes in the advice class
4. Never leak stack traces or internal details in error responses
5. Use `@Valid` on request body parameters for bean validation

## Validation

1. Use Jakarta Bean Validation annotations on request DTOs: `@NotBlank`, `@Size`, `@Email`
2. Validate at the controller boundary — services assume valid input
3. Return 400 with field-level error details for validation failures
4. Custom validators for domain rules (e.g., valid status transitions)

## Pagination

1. Use Spring's `Pageable` for list endpoints
2. Default page size: 20, max: 100
3. Return `Page<T>` response with metadata (totalElements, totalPages, number, size)
4. Support `sort` parameter: `?sort=createdAt,desc`

## Filtering and Search

1. Use query parameters for simple filters: `?status=APPLIED&companyId=uuid`
2. For complex search, accept a search query parameter: `?q=google`
3. Combine filters: all query params are AND conditions
4. Return empty list (not 404) when no results match filters

## Service Layer

1. One service per domain aggregate — services own business logic
2. Use `@Service` with `@Transactional` at class level (readOnly where appropriate)
3. Services work with entities, not DTOs — mapping happens at controller boundary
4. Cross-aggregate coordination happens in services, not controllers
5. Throw domain-specific exceptions (e.g., `CompanyNotFoundException`, `InvalidStatusTransitionException`)
