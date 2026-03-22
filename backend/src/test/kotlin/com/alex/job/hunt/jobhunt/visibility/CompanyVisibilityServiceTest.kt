package com.alex.job.hunt.jobhunt.visibility

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled("Wave 0 stub — Plan 02 implements, requires @SpringBootTest context")
class CompanyVisibilityServiceTest {

    @Test
    fun `owner can set company visibility to PUBLIC`() {
    }

    @Test
    fun `getById returns company when user is owner`() {
    }

    @Test
    fun `getById returns public company to non-owner`() {
    }

    @Test
    fun `getById returns shared company to share recipient`() {
    }

    @Test
    fun `getById throws NotFoundException for private company accessed by non-owner`() {
    }

    @Test
    fun `getById throws NotFoundException for private company with existing share accessed by share recipient`() {
    }

    @Test
    fun `update throws NotFoundException for non-owner even on public company`() {
    }

    @Test
    fun `deleting company cleans up associated shares`() {
    }

    @Test
    fun `changing visibility does not delete existing shares`() {
    }

    @Test
    fun `browsePublic returns only PUBLIC companies with owner email`() {
    }

    @Test
    fun `sharedWithMe returns only companies shared with current user`() {
    }

    @Test
    fun `list returns only owner resources regardless of visibility`() {
    }
}
