package com.alex.job.hunt.jobhunt.visibility

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Wave 0 stub — Plan 02 implements, requires @SpringBootTest context")
class JobVisibilityServiceTest {

    @Test
    fun `owner can set job visibility to PUBLIC`() {
    }

    @Test
    fun `getById returns job when user is owner`() {
    }

    @Test
    fun `getById returns public job to non-owner`() {
    }

    @Test
    fun `getById returns shared job to share recipient`() {
    }

    @Test
    fun `getById throws NotFoundException for private job accessed by non-owner`() {
    }

    @Test
    fun `getById throws NotFoundException for private job with existing share accessed by share recipient`() {
    }

    @Test
    fun `update throws NotFoundException for non-owner even on public job`() {
    }

    @Test
    fun `deleting job cleans up associated shares`() {
    }

    @Test
    fun `changing visibility does not delete existing shares`() {
    }

    @Test
    fun `browsePublic returns only PUBLIC jobs`() {
    }

    @Test
    fun `sharedWithMe returns only jobs shared with current user`() {
    }

    @Test
    fun `list returns only owner resources regardless of visibility`() {
    }
}
