package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.ApplicationEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApplicationRepository : JpaRepository<ApplicationEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): ApplicationEntity?

    fun existsByJobIdAndUserIdAndArchivedFalse(jobId: UUID, userId: UUID): Boolean
}
