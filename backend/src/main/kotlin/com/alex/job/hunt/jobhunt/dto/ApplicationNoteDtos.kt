package com.alex.job.hunt.jobhunt.dto

import com.alex.job.hunt.jobhunt.entity.NoteType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateNoteRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 10000, message = "Content must not exceed 10000 characters")
    val content: String,

    val noteType: NoteType? = null
)

data class UpdateNoteRequest(
    @field:NotBlank(message = "Content is required")
    @field:Size(max = 10000, message = "Content must not exceed 10000 characters")
    val content: String
)

data class NoteResponse(
    val id: UUID,
    val applicationId: UUID,
    val content: String,
    val noteType: NoteType,
    val createdAt: Instant,
    val updatedAt: Instant
)
