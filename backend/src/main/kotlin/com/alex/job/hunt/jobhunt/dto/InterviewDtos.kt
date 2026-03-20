package com.alex.job.hunt.jobhunt.dto

import com.alex.job.hunt.jobhunt.entity.InterviewOutcome
import com.alex.job.hunt.jobhunt.entity.InterviewResult
import com.alex.job.hunt.jobhunt.entity.InterviewStage
import com.alex.job.hunt.jobhunt.entity.InterviewType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateInterviewRequest(
    @field:NotNull(message = "Application ID is required")
    val applicationId: UUID,

    @field:NotNull(message = "Scheduled time is required")
    val scheduledAt: Instant,

    @field:NotNull(message = "Interview type is required")
    val interviewType: InterviewType,

    @field:NotNull(message = "Stage is required")
    val stage: InterviewStage,

    val stageLabel: String? = null,

    @field:Min(1, message = "Duration must be at least 1 minute")
    val durationMinutes: Int? = 60,

    val location: String? = null,

    @field:Size(max = 500, message = "Interviewer names must not exceed 500 characters")
    val interviewerNames: String? = null
)

data class UpdateInterviewRequest(
    val scheduledAt: Instant? = null,
    val interviewType: InterviewType? = null,
    val stage: InterviewStage? = null,
    val stageLabel: String? = null,
    val durationMinutes: Int? = null,
    val location: String? = null,
    val interviewerNames: String? = null,
    val outcome: InterviewOutcome? = null,
    val result: InterviewResult? = null,
    val candidateFeedback: String? = null,
    val companyFeedback: String? = null
)

data class InterviewResponse(
    val id: UUID,
    val applicationId: UUID,
    val roundNumber: Int,
    val scheduledAt: Instant,
    val durationMinutes: Int?,
    val interviewType: InterviewType,
    val stage: InterviewStage,
    val stageLabel: String?,
    val outcome: InterviewOutcome,
    val result: InterviewResult,
    val location: String?,
    val interviewerNames: String?,
    val candidateFeedback: String?,
    val companyFeedback: String?,
    val archived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
