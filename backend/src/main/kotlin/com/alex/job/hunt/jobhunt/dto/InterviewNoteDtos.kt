package com.alex.job.hunt.jobhunt.dto

import com.alex.job.hunt.jobhunt.entity.InterviewNoteType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateInterviewNoteRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 10000, message = "Content must not exceed 10000 characters")
    val content: String,

    val noteType: InterviewNoteType? = null
)

data class UpdateInterviewNoteRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 10000, message = "Content must not exceed 10000 characters")
    val content: String
)

data class InterviewNoteResponse(
    val id: UUID,
    val interviewId: UUID,
    val content: String,
    val noteType: InterviewNoteType,
    val createdAt: Instant,
    val updatedAt: Instant
)
