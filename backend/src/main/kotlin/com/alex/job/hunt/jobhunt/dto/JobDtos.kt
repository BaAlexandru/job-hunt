package com.alex.job.hunt.jobhunt.dto

import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.SalaryPeriod
import com.alex.job.hunt.jobhunt.entity.SalaryType
import com.alex.job.hunt.jobhunt.entity.WorkMode
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateJobRequest(
    @field:NotBlank(message = "Job title is required")
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String,

    val description: String? = null,

    @field:Size(max = 2000, message = "URL must not exceed 2000 characters")
    val url: String? = null,

    val notes: String? = null,

    @field:Size(max = 255, message = "Location must not exceed 255 characters")
    val location: String? = null,

    val workMode: WorkMode? = null,
    val jobType: JobType? = null,
    val companyId: UUID? = null,
    val salaryType: SalaryType? = null,
    val salaryMin: BigDecimal? = null,
    val salaryMax: BigDecimal? = null,

    @field:Size(max = 255, message = "Salary text must not exceed 255 characters")
    val salaryText: String? = null,

    @field:Size(max = 10, message = "Currency must not exceed 10 characters")
    val currency: String? = null,

    val salaryPeriod: SalaryPeriod? = null,
    val closingDate: LocalDate? = null
)

data class UpdateJobRequest(
    @field:NotBlank(message = "Job title is required")
    @field:Size(max = 255, message = "Title must not exceed 255 characters")
    val title: String,

    val description: String? = null,

    @field:Size(max = 2000, message = "URL must not exceed 2000 characters")
    val url: String? = null,

    val notes: String? = null,

    @field:Size(max = 255, message = "Location must not exceed 255 characters")
    val location: String? = null,

    val workMode: WorkMode? = null,
    val jobType: JobType? = null,
    val companyId: UUID? = null,
    val salaryType: SalaryType? = null,
    val salaryMin: BigDecimal? = null,
    val salaryMax: BigDecimal? = null,

    @field:Size(max = 255, message = "Salary text must not exceed 255 characters")
    val salaryText: String? = null,

    @field:Size(max = 10, message = "Currency must not exceed 10 characters")
    val currency: String? = null,

    val salaryPeriod: SalaryPeriod? = null,
    val closingDate: LocalDate? = null
)

data class JobResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val url: String?,
    val notes: String?,
    val location: String?,
    val workMode: WorkMode?,
    val jobType: JobType?,
    val companyId: UUID?,
    val companyName: String?,
    val salaryType: SalaryType?,
    val salaryMin: BigDecimal?,
    val salaryMax: BigDecimal?,
    val salaryText: String?,
    val currency: String?,
    val salaryPeriod: SalaryPeriod?,
    val closingDate: LocalDate?,
    val archived: Boolean,
    val archivedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)
