package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.CreateInterviewNoteRequest
import com.alex.job.hunt.jobhunt.dto.InterviewNoteResponse
import com.alex.job.hunt.jobhunt.dto.UpdateInterviewNoteRequest
import com.alex.job.hunt.jobhunt.entity.InterviewNoteEntity
import com.alex.job.hunt.jobhunt.entity.InterviewNoteType
import com.alex.job.hunt.jobhunt.repository.InterviewNoteRepository
import com.alex.job.hunt.jobhunt.repository.InterviewRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class InterviewNoteService(
    private val noteRepository: InterviewNoteRepository,
    private val interviewRepository: InterviewRepository
) {

    fun create(interviewId: UUID, request: CreateInterviewNoteRequest, userId: UUID): InterviewNoteResponse {
        val interview = interviewRepository.findByIdAndUserId(interviewId, userId)
            ?: throw NotFoundException("Interview not found")
        if (interview.archived) throw NotFoundException("Interview not found")

        val note = InterviewNoteEntity(
            interviewId = interviewId,
            content = request.content,
            noteType = request.noteType ?: InterviewNoteType.GENERAL
        )
        return noteRepository.save(note).toResponse()
    }

    @Transactional(readOnly = true)
    fun list(interviewId: UUID, userId: UUID, pageable: Pageable): Page<InterviewNoteResponse> {
        val interview = interviewRepository.findByIdAndUserId(interviewId, userId)
            ?: throw NotFoundException("Interview not found")
        if (interview.archived) throw NotFoundException("Interview not found")

        return noteRepository.findByInterviewIdOrderByCreatedAtDesc(interviewId, pageable)
            .map { it.toResponse() }
    }

    fun update(interviewId: UUID, noteId: UUID, request: UpdateInterviewNoteRequest, userId: UUID): InterviewNoteResponse {
        val interview = interviewRepository.findByIdAndUserId(interviewId, userId)
            ?: throw NotFoundException("Interview not found")
        if (interview.archived) throw NotFoundException("Interview not found")

        val note = noteRepository.findByIdAndInterviewId(noteId, interviewId)
            ?: throw NotFoundException("Interview note not found")

        note.content = request.content
        note.updatedAt = Instant.now()

        return noteRepository.save(note).toResponse()
    }

    fun delete(interviewId: UUID, noteId: UUID, userId: UUID) {
        val interview = interviewRepository.findByIdAndUserId(interviewId, userId)
            ?: throw NotFoundException("Interview not found")
        if (interview.archived) throw NotFoundException("Interview not found")

        val note = noteRepository.findByIdAndInterviewId(noteId, interviewId)
            ?: throw NotFoundException("Interview note not found")

        noteRepository.delete(note)
    }

    private fun InterviewNoteEntity.toResponse(): InterviewNoteResponse = InterviewNoteResponse(
        id = id!!,
        interviewId = interviewId,
        content = content,
        noteType = noteType,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
