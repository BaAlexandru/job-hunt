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
@Table(name = "interviews")
class InterviewEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "application_id", nullable = false)
    val applicationId: UUID,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "round_number", nullable = false)
    var roundNumber: Int,

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: Instant,

    @Column(name = "duration_minutes")
    var durationMinutes: Int? = 60,

    @Column(name = "interview_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var interviewType: InterviewType,

    @Column(name = "stage", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var stage: InterviewStage,

    @Column(name = "stage_label", length = 255)
    var stageLabel: String? = null,

    @Column(name = "outcome", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var outcome: InterviewOutcome = InterviewOutcome.SCHEDULED,

    @Column(name = "result", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var result: InterviewResult = InterviewResult.PENDING,

    @Column(name = "location", columnDefinition = "TEXT")
    var location: String? = null,

    @Column(name = "interviewer_names", length = 500)
    var interviewerNames: String? = null,

    @Column(name = "candidate_feedback", columnDefinition = "TEXT")
    var candidateFeedback: String? = null,

    @Column(name = "company_feedback", columnDefinition = "TEXT")
    var companyFeedback: String? = null,

    @Column(nullable = false)
    var archived: Boolean = false,

    @Column(name = "archived_at")
    var archivedAt: Instant? = null,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InterviewEntity) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
