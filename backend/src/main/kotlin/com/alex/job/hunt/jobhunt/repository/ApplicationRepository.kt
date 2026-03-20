package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.ApplicationEntity
import com.alex.job.hunt.jobhunt.entity.ApplicationStatus
import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.NoteType
import com.alex.job.hunt.jobhunt.entity.WorkMode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.UUID

interface ApplicationRepository : JpaRepository<ApplicationEntity, UUID> {

    fun findByIdAndUserId(id: UUID, userId: UUID): ApplicationEntity?

    fun existsByJobIdAndUserIdAndArchivedFalse(jobId: UUID, userId: UUID): Boolean

    fun findByUserIdAndArchivedFalse(userId: UUID, pageable: Pageable): Page<ApplicationEntity>

    fun findByUserId(userId: UUID, pageable: Pageable): Page<ApplicationEntity>

    @Query(
        """
        SELECT DISTINCT a FROM ApplicationEntity a
        LEFT JOIN JobEntity j ON j.id = a.jobId AND j.userId = a.userId
        LEFT JOIN CompanyEntity c ON c.id = j.companyId AND c.userId = a.userId
        WHERE a.userId = :userId
        AND (:includeArchived = true OR a.archived = false)
        AND (:#{#statuses == null} = true OR a.status IN :statuses)
        AND (:companyId IS NULL OR j.companyId = :companyId)
        AND (:jobType IS NULL OR j.jobType = :jobType)
        AND (:workMode IS NULL OR j.workMode = :workMode)
        AND (:search IS NULL OR (
            LOWER(a.quickNotes) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
            OR LOWER(j.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
            OR LOWER(j.description) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
            OR EXISTS (
                SELECT 1 FROM ApplicationNoteEntity n
                WHERE n.applicationId = a.id
                AND LOWER(n.content) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
            )
        ))
        AND (CAST(:dateFrom AS date) IS NULL OR a.appliedDate >= :dateFrom)
        AND (CAST(:dateTo AS date) IS NULL OR a.appliedDate <= :dateTo)
        AND (:hasNextAction IS NULL OR (:hasNextAction = true AND a.nextActionDate IS NOT NULL)
            OR (:hasNextAction = false AND a.nextActionDate IS NULL))
        AND (:noteType IS NULL OR EXISTS (
            SELECT 1 FROM ApplicationNoteEntity n2
            WHERE n2.applicationId = a.id AND n2.noteType = :noteType
        ))
        """
    )
    fun findFiltered(
        userId: UUID,
        statuses: List<ApplicationStatus>?,
        companyId: UUID?,
        jobType: JobType?,
        workMode: WorkMode?,
        search: String?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        hasNextAction: Boolean?,
        noteType: NoteType?,
        includeArchived: Boolean,
        pageable: Pageable
    ): Page<ApplicationEntity>
}
