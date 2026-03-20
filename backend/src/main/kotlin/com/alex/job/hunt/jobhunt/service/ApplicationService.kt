package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.ApplicationResponse
import com.alex.job.hunt.jobhunt.dto.CreateApplicationRequest
import com.alex.job.hunt.jobhunt.dto.UpdateApplicationRequest
import com.alex.job.hunt.jobhunt.dto.UpdateStatusRequest
import com.alex.job.hunt.jobhunt.entity.ApplicationEntity
import com.alex.job.hunt.jobhunt.entity.ApplicationStatus
import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.NoteType
import com.alex.job.hunt.jobhunt.entity.WorkMode
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.JobRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class ApplicationService(
    private val applicationRepository: ApplicationRepository,
    private val jobRepository: JobRepository,
    private val companyRepository: CompanyRepository,
    private val noteService: ApplicationNoteService
) {

    private val TERMINAL_STATUSES = setOf(
        ApplicationStatus.REJECTED, ApplicationStatus.ACCEPTED, ApplicationStatus.WITHDRAWN
    )
    private val ACTIVE_STATUSES = ApplicationStatus.entries.toSet() - TERMINAL_STATUSES

    private val transitions: Map<ApplicationStatus, Set<ApplicationStatus>> = mapOf(
        ApplicationStatus.INTERESTED to setOf(ApplicationStatus.APPLIED, ApplicationStatus.WITHDRAWN),
        ApplicationStatus.APPLIED to setOf(
            ApplicationStatus.PHONE_SCREEN, ApplicationStatus.INTERVIEW,
            ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN
        ),
        ApplicationStatus.PHONE_SCREEN to setOf(
            ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER,
            ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN
        ),
        ApplicationStatus.INTERVIEW to setOf(
            ApplicationStatus.PHONE_SCREEN, ApplicationStatus.OFFER,
            ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN
        ),
        ApplicationStatus.OFFER to setOf(
            ApplicationStatus.ACCEPTED, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN
        ),
        ApplicationStatus.REJECTED to ACTIVE_STATUSES,
        ApplicationStatus.ACCEPTED to ACTIVE_STATUSES,
        ApplicationStatus.WITHDRAWN to ACTIVE_STATUSES,
    )

    fun create(request: CreateApplicationRequest, userId: UUID): ApplicationResponse {
        val job = jobRepository.findByIdAndUserId(request.jobId, userId)
            ?: throw NotFoundException("Job not found")

        if (applicationRepository.existsByJobIdAndUserIdAndArchivedFalse(request.jobId, userId)) {
            throw ConflictException("Application already exists for this job")
        }

        val entity = ApplicationEntity(
            userId = userId,
            jobId = request.jobId,
            quickNotes = request.quickNotes,
            appliedDate = request.appliedDate,
            nextActionDate = request.nextActionDate
        )
        val saved = applicationRepository.save(entity)

        val companyName = job.companyId?.let { companyRepository.findByIdAndUserId(it, userId)?.name }
        return saved.toResponse(job.title, companyName)
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID, userId: UUID): ApplicationResponse {
        val entity = findOwnedApplication(id, userId)
        val job = jobRepository.findByIdAndUserId(entity.jobId, entity.userId)
        val companyName = job?.companyId?.let { companyRepository.findByIdAndUserId(it, entity.userId)?.name }
        val jobTitle = job?.title ?: "Unknown"
        return entity.toResponse(jobTitle, companyName)
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID, includeArchived: Boolean, pageable: Pageable): Page<ApplicationResponse> {
        val page = if (includeArchived) {
            applicationRepository.findByUserId(userId, pageable)
        } else {
            applicationRepository.findByUserIdAndArchivedFalse(userId, pageable)
        }

        val jobIds = page.content.map { it.jobId }.toSet()
        val jobMap = if (jobIds.isNotEmpty()) {
            jobRepository.findAllByIdIn(jobIds).associateBy { it.id!! }
        } else emptyMap()

        val companyIds = jobMap.values.mapNotNull { it.companyId }.toSet()
        val companyNameMap = if (companyIds.isNotEmpty()) {
            companyRepository.findAllByIdInAndUserId(companyIds, userId).associate { it.id!! to it.name }
        } else emptyMap()

        return page.map { entity ->
            val job = jobMap[entity.jobId]
            val companyName = job?.companyId?.let { companyNameMap[it] }
            entity.toResponse(job?.title ?: "Unknown", companyName)
        }
    }

    @Transactional(readOnly = true)
    fun listFiltered(
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
    ): Page<ApplicationResponse> {
        val effectiveStatuses = if (statuses.isNullOrEmpty()) null else statuses

        val page = applicationRepository.findFiltered(
            userId, effectiveStatuses, companyId, jobType, workMode,
            search, dateFrom, dateTo, hasNextAction, noteType,
            includeArchived, pageable
        )

        val jobIds = page.content.map { it.jobId }.toSet()
        val jobMap = if (jobIds.isNotEmpty()) {
            jobRepository.findAllByIdIn(jobIds).associateBy { it.id!! }
        } else emptyMap()
        val companyIds = jobMap.values.mapNotNull { it.companyId }.toSet()
        val companyNameMap = if (companyIds.isNotEmpty()) {
            companyRepository.findAllByIdInAndUserId(companyIds, userId)
                .associate { it.id!! to it.name }
        } else emptyMap()

        return page.map { entity ->
            val job = jobMap[entity.jobId]
            entity.toResponse(
                jobTitle = job?.title ?: "Unknown",
                companyName = job?.companyId?.let { companyNameMap[it] }
            )
        }
    }

    fun update(id: UUID, request: UpdateApplicationRequest, userId: UUID): ApplicationResponse {
        val entity = findOwnedApplication(id, userId)

        entity.quickNotes = request.quickNotes
        entity.appliedDate = request.appliedDate
        entity.nextActionDate = request.nextActionDate
        entity.updatedAt = Instant.now()

        val saved = applicationRepository.save(entity)
        val job = jobRepository.findByIdAndUserId(entity.jobId, entity.userId)
        val companyName = job?.companyId?.let { companyRepository.findByIdAndUserId(it, entity.userId)?.name }
        val jobTitle = job?.title ?: "Unknown"
        return saved.toResponse(jobTitle, companyName)
    }

    fun updateStatus(id: UUID, request: UpdateStatusRequest, userId: UUID): ApplicationResponse {
        val entity = findOwnedApplication(id, userId)
        val oldStatus = entity.status

        validateTransition(oldStatus, request.status)

        entity.status = request.status
        entity.lastActivityDate = Instant.now()
        entity.updatedAt = Instant.now()

        if (request.status == ApplicationStatus.APPLIED && entity.appliedDate == null) {
            entity.appliedDate = LocalDate.now()
        }

        val saved = applicationRepository.save(entity)

        noteService.createStatusChangeNote(entity.id!!, oldStatus, request.status)

        val job = jobRepository.findByIdAndUserId(entity.jobId, entity.userId)
        val companyName = job?.companyId?.let { companyRepository.findByIdAndUserId(it, entity.userId)?.name }
        val jobTitle = job?.title ?: "Unknown"
        return saved.toResponse(jobTitle, companyName)
    }

    fun archive(id: UUID, userId: UUID) {
        val entity = findOwnedApplication(id, userId)
        entity.archived = true
        entity.archivedAt = Instant.now()
        entity.updatedAt = Instant.now()
        applicationRepository.save(entity)
    }

    fun getValidTransitions(id: UUID, userId: UUID): Set<ApplicationStatus> {
        val entity = findOwnedApplication(id, userId)
        return transitions[entity.status] ?: emptySet()
    }

    private fun validateTransition(from: ApplicationStatus, to: ApplicationStatus) {
        val allowed = transitions[from] ?: emptySet()
        if (to !in allowed) {
            throw InvalidTransitionException("Cannot transition from $from to $to")
        }
    }

    private fun findOwnedApplication(id: UUID, userId: UUID): ApplicationEntity {
        return applicationRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Application not found")
    }

    private fun ApplicationEntity.toResponse(jobTitle: String, companyName: String?): ApplicationResponse =
        ApplicationResponse(
            id = id!!,
            jobId = jobId,
            jobTitle = jobTitle,
            companyName = companyName,
            status = status,
            quickNotes = quickNotes,
            appliedDate = appliedDate,
            lastActivityDate = lastActivityDate,
            nextActionDate = nextActionDate,
            archived = archived,
            archivedAt = archivedAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
}
