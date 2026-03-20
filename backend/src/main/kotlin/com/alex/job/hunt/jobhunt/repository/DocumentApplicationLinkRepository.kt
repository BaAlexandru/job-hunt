package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.DocumentApplicationLinkEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DocumentApplicationLinkRepository : JpaRepository<DocumentApplicationLinkEntity, UUID> {
    fun findByApplicationId(applicationId: UUID): List<DocumentApplicationLinkEntity>
    fun findByDocumentVersionId(documentVersionId: UUID): List<DocumentApplicationLinkEntity>
    fun existsByDocumentVersionIdAndApplicationId(documentVersionId: UUID, applicationId: UUID): Boolean
    fun deleteByDocumentVersionIdAndApplicationId(documentVersionId: UUID, applicationId: UUID)
}
