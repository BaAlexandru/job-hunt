package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.DocumentCategory
import com.alex.job.hunt.jobhunt.entity.DocumentEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface DocumentRepository : JpaRepository<DocumentEntity, UUID> {
    fun findByIdAndUserIdAndArchivedFalse(id: UUID, userId: UUID): DocumentEntity?

    @Query("""
        SELECT d FROM DocumentEntity d
        WHERE d.userId = :userId
        AND d.archived = false
        AND (:category IS NULL OR d.category = :category)
        AND (:search IS NULL OR LOWER(d.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(d.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
    """)
    fun findAllByFilters(
        @Param("userId") userId: UUID,
        @Param("category") category: DocumentCategory?,
        @Param("search") search: String?,
        pageable: Pageable
    ): Page<DocumentEntity>
}
