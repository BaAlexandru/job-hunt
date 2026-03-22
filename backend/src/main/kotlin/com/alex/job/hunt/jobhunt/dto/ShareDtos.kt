package com.alex.job.hunt.jobhunt.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class CreateShareRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String
)

data class ShareResponse(
    val id: UUID,
    val email: String,
    val sharedAt: Instant
)
