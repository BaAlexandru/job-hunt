package com.alex.job.hunt.jobhunt.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateCompanyRequest(
    @field:NotBlank(message = "Company name is required")
    @field:Size(max = 255, message = "Company name must not exceed 255 characters")
    val name: String,

    @field:Size(max = 500, message = "Website must not exceed 500 characters")
    val website: String? = null,

    @field:Size(max = 255, message = "Location must not exceed 255 characters")
    val location: String? = null,

    val notes: String? = null
)

data class UpdateCompanyRequest(
    @field:NotBlank(message = "Company name is required")
    @field:Size(max = 255, message = "Company name must not exceed 255 characters")
    val name: String,

    @field:Size(max = 500, message = "Website must not exceed 500 characters")
    val website: String? = null,

    @field:Size(max = 255, message = "Location must not exceed 255 characters")
    val location: String? = null,

    val notes: String? = null
)

data class CompanyResponse(
    val id: UUID,
    val name: String,
    val website: String?,
    val location: String?,
    val notes: String?,
    val archived: Boolean,
    val archivedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
