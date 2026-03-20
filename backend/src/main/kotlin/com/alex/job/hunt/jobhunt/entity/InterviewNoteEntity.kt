package com.alex.job.hunt.jobhunt.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "interview_notes")
class InterviewNoteEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "interview_id", nullable = false)
    val interviewId: UUID,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(name = "note_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var noteType: InterviewNoteType = InterviewNoteType.GENERAL,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InterviewNoteEntity) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
