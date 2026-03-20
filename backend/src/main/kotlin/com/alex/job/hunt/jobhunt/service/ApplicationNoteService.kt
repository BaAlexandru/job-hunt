package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.CreateNoteRequest
import com.alex.job.hunt.jobhunt.dto.NoteResponse
import com.alex.job.hunt.jobhunt.dto.UpdateNoteRequest
import com.alex.job.hunt.jobhunt.entity.ApplicationNoteEntity
import com.alex.job.hunt.jobhunt.entity.ApplicationStatus
import com.alex.job.hunt.jobhunt.entity.NoteType
import com.alex.job.hunt.jobhunt.repository.ApplicationNoteRepository
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class ApplicationNoteService(
    private val noteRepository: ApplicationNoteRepository,
    private val applicationRepository: ApplicationRepository
) {

    fun create(applicationId: UUID, request: CreateNoteRequest, userId: UUID): NoteResponse {
        val application = applicationRepository.findByIdAndUserId(applicationId, userId)
            ?: throw NotFoundException("Application not found")

        val note = ApplicationNoteEntity(
            applicationId = applicationId,
            content = request.content,
            noteType = request.noteType ?: NoteType.GENERAL
        )
        val savedNote = noteRepository.save(note)

        application.lastActivityDate = Instant.now()
        application.updatedAt = Instant.now()
        applicationRepository.save(application)

        return savedNote.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(applicationId: UUID, userId: UUID, pageable: Pageable): Page<NoteResponse> {
        applicationRepository.findByIdAndUserId(applicationId, userId)
            ?: throw NotFoundException("Application not found")

        return noteRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId, pageable)
            .map { it.toResponse() }
    }

    fun update(applicationId: UUID, noteId: UUID, request: UpdateNoteRequest, userId: UUID): NoteResponse {
        applicationRepository.findByIdAndUserId(applicationId, userId)
            ?: throw NotFoundException("Application not found")

        val note = noteRepository.findByIdAndApplicationId(noteId, applicationId)
            ?: throw NotFoundException("Note not found")

        note.content = request.content
        note.updatedAt = Instant.now()

        return noteRepository.save(note).toResponse()
    }

    fun delete(applicationId: UUID, noteId: UUID, userId: UUID) {
        applicationRepository.findByIdAndUserId(applicationId, userId)
            ?: throw NotFoundException("Application not found")

        val note = noteRepository.findByIdAndApplicationId(noteId, applicationId)
            ?: throw NotFoundException("Note not found")

        if (note.noteType == NoteType.STATUS_CHANGE) {
            throw ConflictException("Cannot delete status change notes")
        }

        noteRepository.delete(note)
    }

    fun createStatusChangeNote(applicationId: UUID, oldStatus: ApplicationStatus, newStatus: ApplicationStatus) {
        val note = ApplicationNoteEntity(
            applicationId = applicationId,
            content = "Status changed: $oldStatus -> $newStatus",
            noteType = NoteType.STATUS_CHANGE
        )
        noteRepository.save(note)
    }

    private fun ApplicationNoteEntity.toResponse(): NoteResponse = NoteResponse(
        id = id!!,
        applicationId = applicationId,
        content = content,
        noteType = noteType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
