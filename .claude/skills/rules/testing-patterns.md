# Testing Patterns

## Test Structure

1. Test files mirror source structure in `src/test/kotlin/`
2. Test class naming: `{ClassName}Tests.kt` (plural, matches Spring convention)
3. Test method naming: `should {expected behavior} when {condition}`
4. One assertion per test when practical — prefer many focused tests over few large ones
5. Arrange-Act-Assert pattern with blank lines separating sections

## Test Types

### Integration Tests (`@SpringBootTest`)
- Full application context with real database
- Used for: service layer logic, repository queries, end-to-end flows
- Docker Compose auto-starts PostgreSQL (skip.in-tests: false)
- Each test gets a clean transaction (rolled back by default)

### Controller Tests (`@WebMvcTest`)
- Slice test — only loads web layer for the specified controller
- Mock service dependencies with MockK (`@MockkBean`)
- Used for: request/response mapping, validation, error handling, security
- Test HTTP status codes, response shapes, and error cases

### Repository Tests (`@DataJpaTest`)
- Slice test — only loads JPA and database layer
- Used for: custom queries, complex JPQL, native SQL
- Flyway runs migrations against test database automatically

## Test Dependencies (add in Phase 2)

```kotlin
testImplementation("io.mockk:mockk:1.13+")
testImplementation("com.ninja-squad:springmockk:4.0+")
testImplementation("org.springframework.security:spring-security-test")
```
- MockK over Mockito — idiomatic Kotlin mocking
- SpringMockK provides `@MockkBean` and `@SpykBean` for Spring integration
- spring-security-test for `@WithMockUser` and security testing

## MockK Conventions

1. Use `mockk<ServiceClass>()` for creating mocks
2. Use `every { mock.method() } returns value` for stubbing
3. Use `verify { mock.method() }` for behavior verification
4. Use `relaxed = true` sparingly — prefer explicit stubs
5. Use `@MockkBean` in Spring tests (from springmockk), not `@MockBean`

## Test Data

1. Use helper functions for creating test entities:
   ```kotlin
   fun aCompany(
       name: String = "Test Corp",
       website: String? = null
   ) = Company(name = name, website = website, userId = testUserId)
   ```
2. Default values for optional fields — override only what the test cares about
3. Use `UUID.randomUUID()` for test IDs when not relying on database generation
4. Keep test data minimal — only set fields relevant to the test

## Controller Test Pattern

```kotlin
@WebMvcTest(CompanyController::class)
class CompanyControllerTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var companyService: CompanyService

    @Test
    fun `should return 201 when creating valid company`() {
        every { companyService.create(any()) } returns aCompany()

        mockMvc.post("/api/companies") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name": "Test Corp"}"""
        }.andExpect {
            status { isCreated() }
        }
    }
}
```

## Integration Test Pattern

```kotlin
@SpringBootTest
@Transactional
class CompanyServiceTests {

    @Autowired
    lateinit var companyService: CompanyService

    @Test
    fun `should create company and assign to user`() {
        val company = companyService.create(CreateCompanyRequest("Test Corp"), userId)

        assertThat(company.name).isEqualTo("Test Corp")
        assertThat(company.userId).isEqualTo(userId)
    }
}
```

## Security Testing (Phase 2+)

1. Use `@WithMockUser` for authenticated endpoint tests
2. Test that unauthenticated requests return 401
3. Test that accessing other users' resources returns 403
4. Test JWT token validation edge cases (expired, malformed, missing)

## TDD Workflow

1. Write a failing test FIRST (Red)
2. Write minimal implementation to pass (Green)
3. Refactor while tests stay green (Refactor)
4. Never skip the red step — if the test passes immediately, it's not testing anything new
5. Run `./gradlew :backend:test` before marking any task complete

## Assertions

1. Use AssertJ for fluent assertions: `assertThat(result).isEqualTo(expected)`
2. Use `assertThatThrownBy { }` for exception testing
3. For collections: `assertThat(list).hasSize(3).extracting("name").contains("Test")`
4. Avoid assertTrue/assertFalse — use descriptive AssertJ matchers
