package com.alex.job.hunt.jobhunt.dto

import com.alex.job.hunt.jobhunt.entity.ApplicationStatus
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateApplicationRequest(
    @field:NotNull(message = "Job ID is required")
    val jobId: UUID,

    val quickNotes: String? = null,
    val appliedDate: LocalDate? = null,
    val nextActionDate: LocalDate? = null
)

data class UpdateApplicationRequest(
    val quickNotes: String? = null,
    val appliedDate: LocalDate? = null,
    val nextActionDate: LocalDate? = null
)

data class UpdateStatusRequest(
    @field:NotNull(message = "Status is required")
    val status: ApplicationStatus
)

data class ApplicationResponse(
    val id: UUID,
    val jobId: UUID,
    val jobTitle: String,
    val companyName: String?,
    val status: ApplicationStatus,
    val quickNotes: String?,
    val appliedDate: LocalDate?,
    val lastActivityDate: Instant,
    val nextActionDate: LocalDate?,
    val archived: Boolean,
    val archivedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
