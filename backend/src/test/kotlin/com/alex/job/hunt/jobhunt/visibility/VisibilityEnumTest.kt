package com.alex.job.hunt.jobhunt.visibility

import com.alex.job.hunt.jobhunt.entity.ResourceType
import com.alex.job.hunt.jobhunt.entity.Visibility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VisibilityEnumTest {

    @Test
    fun `Visibility enum has PRIVATE PUBLIC SHARED values`() {
        val values = Visibility.entries.map { it.name }
        assertEquals(3, values.size)
        assertTrue(values.contains("PRIVATE"))
        assertTrue(values.contains("PUBLIC"))
        assertTrue(values.contains("SHARED"))
    }

    @Test
    fun `ResourceType enum has COMPANY JOB values`() {
        val values = ResourceType.entries.map { it.name }
        assertEquals(2, values.size)
        assertTrue(values.contains("COMPANY"))
        assertTrue(values.contains("JOB"))
    }
}
