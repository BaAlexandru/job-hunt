package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.JobEntity
import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.ResourceShareEntity
import com.alex.job.hunt.jobhunt.entity.Visibility
import com.alex.job.hunt.jobhunt.entity.WorkMode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface JobRepository : JpaRepository<JobEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): JobEntity?

    fun findAllByIdIn(ids: Set<UUID>): List<JobEntity>

    fun existsByCompanyIdAndUserIdAndArchivedFalse(companyId: UUID, userId: UUID): Boolean

    @Query(
        """
        SELECT j FROM JobEntity j
        WHERE j.userId = :userId
        AND (:includeArchived = true OR j.archived = false)
        AND (:companyId IS NULL OR j.companyId = :companyId)
        AND (:jobType IS NULL OR j.jobType = :jobType)
        AND (:workMode IS NULL OR j.workMode = :workMode)
        AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
        """
    )
    fun findFiltered(
        userId: UUID,
        companyId: UUID?,
        jobType: JobType?,
        workMode: WorkMode?,
        title: String?,
        includeArchived: Boolean,
        pageable: Pageable
    ): Page<JobEntity>

    @Query(
        """
        SELECT j FROM JobEntity j
        WHERE j.id = :id
        AND (
            j.userId = :userId
            OR j.visibility = com.alex.job.hunt.jobhunt.entity.Visibility.PUBLIC
            OR (j.visibility = com.alex.job.hunt.jobhunt.entity.Visibility.SHARED AND EXISTS (
                SELECT 1 FROM ResourceShareEntity s
                WHERE s.resourceType = com.alex.job.hunt.jobhunt.entity.ResourceType.JOB
                AND s.resourceId = j.id
                AND s.sharedWithId = :userId
            ))
        )
        """
    )
    fun findByIdWithVisibility(id: UUID, userId: UUID): JobEntity?

    @Query(
        """
        SELECT j FROM JobEntity j
        WHERE j.visibility = com.alex.job.hunt.jobhunt.entity.Visibility.PUBLIC
        AND (:title IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:title AS string), '%')))
        """
    )
    fun findPublic(title: String?, pageable: Pageable): Page<JobEntity>

    @Query(
        """
        SELECT j FROM JobEntity j
        WHERE j.id IN (
            SELECT s.resourceId FROM ResourceShareEntity s
            WHERE s.sharedWithId = :userId
            AND s.resourceType = com.alex.job.hunt.jobhunt.entity.ResourceType.JOB
        )
        """
    )
    fun findSharedWithUser(userId: UUID, pageable: Pageable): Page<JobEntity>
}
