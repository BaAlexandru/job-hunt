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
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "applications")
class ApplicationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    val userId: UUID,

    @Column(name = "job_id", nullable = false)
    val jobId: UUID,

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    var status: ApplicationStatus = ApplicationStatus.INTERESTED,

    @Column(name = "quick_notes", columnDefinition = "TEXT")
    var quickNotes: String? = null,

    @Column(name = "applied_date")
    var appliedDate: LocalDate? = null,

    @Column(name = "last_activity_date", nullable = false)
    var lastActivityDate: Instant = Instant.now(),

    @Column(name = "next_action_date")
    var nextActionDate: LocalDate? = null,

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
        if (other !is ApplicationEntity) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
