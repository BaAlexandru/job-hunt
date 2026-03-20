package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.ApplicationNoteEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApplicationNoteRepository : JpaRepository<ApplicationNoteEntity, UUID> {

    fun findByIdAndApplicationId(id: UUID, applicationId: UUID): ApplicationNoteEntity?

    fun findByApplicationIdOrderByCreatedAtDesc(applicationId: UUID, pageable: Pageable): Page<ApplicationNoteEntity>

    fun findByApplicationIdOrderByCreatedAtDesc(applicationId: UUID): List<ApplicationNoteEntity>
}
