package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.DocumentVersionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface DocumentVersionRepository : JpaRepository<DocumentVersionEntity, UUID> {
    fun findByDocumentIdOrderByVersionNumberDesc(documentId: UUID): List<DocumentVersionEntity>
    fun findByDocumentIdAndIsCurrent(documentId: UUID, isCurrent: Boolean): DocumentVersionEntity?
    fun findByIdAndDocumentId(id: UUID, documentId: UUID): DocumentVersionEntity?
    fun countByDocumentId(documentId: UUID): Int

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM DocumentVersionEntity v WHERE v.documentId = :documentId")
    fun findMaxVersionNumberByDocumentId(@Param("documentId") documentId: UUID): Int

    fun findByDocumentIdInAndIsCurrent(documentIds: Collection<UUID>, isCurrent: Boolean): List<DocumentVersionEntity>

    @Modifying
    @Query("UPDATE DocumentVersionEntity v SET v.isCurrent = false WHERE v.documentId = :documentId AND v.isCurrent = true")
    fun clearCurrentFlag(@Param("documentId") documentId: UUID)
}
