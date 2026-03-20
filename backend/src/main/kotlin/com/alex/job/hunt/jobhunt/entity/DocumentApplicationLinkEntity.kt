package com.alex.job.hunt.jobhunt.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "document_application_links")
class DocumentApplicationLinkEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "document_version_id", nullable = false)
    val documentVersionId: UUID,

    @Column(name = "application_id", nullable = false)
    val applicationId: UUID,

    @Column(name = "linked_at", updatable = false, nullable = false)
    val linkedAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DocumentApplicationLinkEntity) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
