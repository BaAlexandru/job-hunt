package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.CompanyEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface CompanyRepository : JpaRepository<CompanyEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): CompanyEntity?

    fun findByIdAndUserIdAndArchivedFalse(id: UUID, userId: UUID): CompanyEntity?

    fun findByUserIdAndArchivedFalse(userId: UUID, pageable: Pageable): Page<CompanyEntity>

    fun findByUserId(userId: UUID, pageable: Pageable): Page<CompanyEntity>

    fun findAllByIdInAndUserId(ids: Collection<UUID>, userId: UUID): List<CompanyEntity>

    @Query(
        """
        SELECT c FROM CompanyEntity c
        WHERE c.userId = :userId
        AND (:includeArchived = true OR c.archived = false)
        AND (:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%')))
        """
    )
    fun findFiltered(
        userId: UUID,
        name: String?,
        includeArchived: Boolean,
        pageable: Pageable
    ): Page<CompanyEntity>
}
