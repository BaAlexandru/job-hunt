package com.alex.job.hunt.jobhunt.visibility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VisibilityMigrationTest {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `V16 migration adds visibility column to companies with PRIVATE default`() {
        val defaultValue = jdbcTemplate.queryForObject(
            "SELECT column_default FROM information_schema.columns WHERE table_name = 'companies' AND column_name = 'visibility'",
            String::class.java
        )
        assertEquals("'PRIVATE'::character varying", defaultValue)
    }

    @Test
    fun `V16 migration adds visibility column to jobs with PRIVATE default`() {
        val defaultValue = jdbcTemplate.queryForObject(
            "SELECT column_default FROM information_schema.columns WHERE table_name = 'jobs' AND column_name = 'visibility'",
            String::class.java
        )
        assertEquals("'PRIVATE'::character varying", defaultValue)
    }

    @Test
    fun `V16 migration creates resource_shares table`() {
        val exists = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'resource_shares'",
            Int::class.java
        )
        assertTrue(exists!! > 0)
    }
}
