package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.InterviewNoteEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InterviewNoteRepository : JpaRepository<InterviewNoteEntity, UUID> {

    fun findByIdAndInterviewId(id: UUID, interviewId: UUID): InterviewNoteEntity?

    fun findByInterviewIdOrderByCreatedAtDesc(interviewId: UUID, pageable: Pageable): Page<InterviewNoteEntity>

    fun findByInterviewIdOrderByCreatedAtDesc(interviewId: UUID): List<InterviewNoteEntity>

    fun findByInterviewIdIn(interviewIds: List<UUID>): List<InterviewNoteEntity>
}
