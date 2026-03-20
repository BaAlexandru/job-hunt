package com.alex.job.hunt.jobhunt.dto

import com.alex.job.hunt.jobhunt.entity.DocumentCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class DocumentResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val category: DocumentCategory,
    val currentVersion: DocumentVersionResponse?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class DocumentVersionResponse(
    val id: UUID,
    val versionNumber: Int,
    val originalFilename: String,
    val contentType: String,
    val fileSize: Long,
    val note: String?,
    val isCurrent: Boolean,
    val createdAt: Instant
)

data class DocumentApplicationLinkResponse(
    val id: UUID,
    val documentVersionId: UUID,
    val applicationId: UUID,
    val linkedAt: Instant,
    val versionRemoved: Boolean = false
)

data class DocumentUpdateRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 255, message = "Title must be at most 255 characters")
    val title: String,
    val description: String?,
    val category: DocumentCategory
)

data class LinkDocumentRequest(
    @field:NotNull(message = "Document version ID is required")
    val documentVersionId: UUID,
    @field:NotNull(message = "Application ID is required")
    val applicationId: UUID
)
