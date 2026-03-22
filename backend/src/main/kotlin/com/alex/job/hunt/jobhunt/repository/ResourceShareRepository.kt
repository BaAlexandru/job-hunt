package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.ResourceShareEntity
import com.alex.job.hunt.jobhunt.entity.ResourceType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ResourceShareRepository : JpaRepository<ResourceShareEntity, UUID> {

    fun existsByResourceTypeAndResourceIdAndSharedWithId(
        resourceType: ResourceType,
        resourceId: UUID,
        sharedWithId: UUID
    ): Boolean

    fun findByResourceTypeAndResourceId(
        resourceType: ResourceType,
        resourceId: UUID
    ): List<ResourceShareEntity>

    fun deleteByResourceTypeAndResourceId(
        resourceType: ResourceType,
        resourceId: UUID
    )

    @Query(
        """
        SELECT s FROM ResourceShareEntity s
        WHERE s.sharedWithId = :userId AND s.resourceType = :resourceType
        """
    )
    fun findSharedWithUser(
        userId: UUID,
        resourceType: ResourceType,
        pageable: Pageable
    ): Page<ResourceShareEntity>
}
