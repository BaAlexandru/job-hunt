package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.InterviewEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface InterviewRepository : JpaRepository<InterviewEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): InterviewEntity?

    fun findByApplicationIdAndUserIdAndArchivedFalseOrderByScheduledAtAsc(
        applicationId: UUID,
        userId: UUID,
        pageable: Pageable
    ): Page<InterviewEntity>

    fun findByApplicationIdAndArchivedFalse(applicationId: UUID): List<InterviewEntity>

    @Query("SELECT COALESCE(MAX(i.roundNumber), 0) FROM InterviewEntity i WHERE i.applicationId = :applicationId")
    fun findMaxRoundNumberByApplicationId(applicationId: UUID): Int

    @Query("SELECT i.id FROM InterviewEntity i WHERE i.applicationId = :applicationId AND i.archived = false")
    fun findIdsByApplicationId(applicationId: UUID): List<UUID>
}
