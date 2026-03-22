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
@Table(name = "resource_shares")
class ResourceShareEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    val id: UUID? = null,

    @Column(name = "resource_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    val resourceType: ResourceType,

    @Column(name = "resource_id", nullable = false)
    val resourceId: UUID,

    @Column(name = "owner_id", nullable = false)
    val ownerId: UUID,

    @Column(name = "shared_with_id", nullable = false)
    val sharedWithId: UUID,

    @Column(name = "created_at", updatable = false, nullable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResourceShareEntity) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
