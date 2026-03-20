# JPA Entity Patterns

## Entity Structure

1. All entities are Kotlin classes (not data classes) — JPA requires mutable proxies
2. Primary constructor with all required fields; use `lateinit` sparingly
3. Always annotate with `@Entity` and `@Table(name = "table_name")`
4. Use `plugin.jpa` (allOpen) — entities are automatically opened for proxying

## Primary Keys

1. UUID primary keys on every table
2. Database-generated via `gen_random_uuid()` — never application-generated
3. Standard mapping:
   ```kotlin
   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
   @Column(updatable = false, nullable = false)
   val id: UUID? = null
   ```
4. `id` is nullable (`UUID?`) with default `null` — set by DB on insert

## Audit Columns

Every entity includes:
```kotlin
@Column(name = "created_at", updatable = false, nullable = false)
val createdAt: Instant = Instant.now()

@Column(name = "updated_at", nullable = false)
var updatedAt: Instant = Instant.now()
```
- `created_at` is immutable (`val`), set once
- `updated_at` is mutable (`var`), updated on modification
- Use `Instant` (not `LocalDateTime`) for timezone safety
- Consider `@PreUpdate` callback for auto-updating `updatedAt`

## Multi-User Design

1. All domain entities have a `user_id` foreign key
2. Map as a `@ManyToOne` relationship to the User entity
3. Never expose user_id in public API responses — derive from JWT
4. Query by user context: `findByUserId(userId)` repository methods

## Relationships

1. Prefer `@ManyToOne` on the owning side; avoid bidirectional unless needed for queries
2. If bidirectional, use `mappedBy` on the inverse side and manage both sides in code
3. Use `FetchType.LAZY` for all `@OneToMany` and `@ManyToMany` — never EAGER
4. `@ManyToOne` defaults to EAGER — explicitly set `FetchType.LAZY`
5. Cascade only where ownership is clear (e.g., `Application` owns `Note`)

## equals / hashCode

1. Use business key or `id`-based equality — never all fields
2. Recommended pattern for entities with DB-generated IDs:
   ```kotlin
   override fun equals(other: Any?): Boolean {
       if (this === other) return true
       if (other !is EntityClass) return false
       return id != null && id == other.id
   }

   override fun hashCode(): Int = javaClass.hashCode()
   ```
3. Constant `hashCode()` ensures consistency across proxy/managed state transitions

## Enums

1. Map enums with `@Enumerated(EnumType.STRING)` — never ORDINAL
2. Store as `VARCHAR` in PostgreSQL, not integer
3. Define enum classes in the same file as the entity or in a shared `model` package

## Column Mapping

1. Explicit `@Column(name = "snake_case")` on every field — don't rely on implicit naming
2. Use `nullable = false` on required columns
3. Use `@Column(length = N)` for bounded strings
4. Use `columnDefinition = "TEXT"` for unbounded text fields
5. Map PostgreSQL `TIMESTAMPTZ` to Kotlin `Instant`

## Flyway Alignment

1. Every new entity requires a corresponding Flyway migration FIRST
2. Migration naming: `V{N}__{phaseNN}_{description}.sql`
3. `ddl-auto=validate` ensures entity mappings match the migration-created schema
4. Never use `@Table` without a matching `CREATE TABLE` in a migration
