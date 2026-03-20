package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.JobEntity
import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.WorkMode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface JobRepository : JpaRepository<JobEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): JobEntity?

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
}
