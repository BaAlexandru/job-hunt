package com.alex.job.hunt.jobhunt.visibility

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Wave 0 stub — Plan 02 implements, requires @SpringBootTest context")
class ResourceShareServiceTest {

    @Test
    fun `createShare with valid email creates share and returns ShareResponse`() {
    }

    @Test
    fun `createShare with unknown email throws NotFoundException`() {
    }

    @Test
    fun `createShare with own email throws ConflictException`() {
    }

    @Test
    fun `createShare duplicate throws ConflictException`() {
    }

    @Test
    fun `revokeShare by owner succeeds`() {
    }

    @Test
    fun `revokeShare by non-owner throws NotFoundException`() {
    }

    @Test
    fun `listShares returns all shares for resource`() {
    }
}
