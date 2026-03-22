package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.BrowseJobResponse
import com.alex.job.hunt.jobhunt.dto.CreateJobRequest
import com.alex.job.hunt.jobhunt.dto.JobResponse
import com.alex.job.hunt.jobhunt.dto.UpdateJobRequest
import com.alex.job.hunt.jobhunt.entity.JobEntity
import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.ResourceType
import com.alex.job.hunt.jobhunt.entity.Visibility
import com.alex.job.hunt.jobhunt.entity.WorkMode
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.JobRepository
import com.alex.job.hunt.jobhunt.repository.ResourceShareRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class JobService(
    private val jobRepository: JobRepository,
    private val companyRepository: CompanyRepository,
    private val userRepository: UserRepository,
    private val resourceShareRepository: ResourceShareRepository
) {

    fun create(request: CreateJobRequest, userId: UUID): JobResponse {
        val companyName = request.companyId?.let { validateCompany(it, userId) }

        val entity = JobEntity(
            userId = userId,
            companyId = request.companyId,
            title = request.title,
            description = request.description,
            url = request.url,
            notes = request.notes,
            location = request.location,
            workMode = request.workMode,
            jobType = request.jobType,
            salaryType = request.salaryType,
            salaryMin = request.salaryMin,
            salaryMax = request.salaryMax,
            salaryText = request.salaryText,
            currency = request.currency,
            salaryPeriod = request.salaryPeriod,
            closingDate = request.closingDate
        )
        return jobRepository.save(entity).toResponse(companyName, isOwner = true)
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID, userId: UUID): JobResponse {
        val entity = jobRepository.findByIdWithVisibility(id, userId)
            ?: throw NotFoundException("Job not found")
        val isOwner = entity.userId == userId
        val companyName = entity.companyId?.let { resolveCompanyName(it) }
        return entity.toResponse(companyName, isOwner = isOwner)
    }

    @Transactional(readOnly = true)
    fun list(
        userId: UUID,
        companyId: UUID?,
        jobType: JobType?,
        workMode: WorkMode?,
        title: String?,
        includeArchived: Boolean,
        pageable: Pageable
    ): Page<JobResponse> {
        val page = jobRepository.findFiltered(userId, companyId, jobType, workMode, title, includeArchived, pageable)
        val companyIds = page.content.mapNotNull { it.companyId }.toSet()
        val companyNameMap = if (companyIds.isNotEmpty()) {
            companyRepository.findAllByIdInAndUserId(companyIds, userId)
                .associate { it.id!! to it.name }
        } else {
            emptyMap()
        }
        return page.map { it.toResponse(companyNameMap[it.companyId], isOwner = true) }
    }

    fun update(id: UUID, request: UpdateJobRequest, userId: UUID): JobResponse {
        val entity = jobRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Job not found")

        val companyName = request.companyId?.let { validateCompany(it, userId) }

        entity.companyId = request.companyId
        entity.title = request.title
        entity.description = request.description
        entity.url = request.url
        entity.notes = request.notes
        entity.location = request.location
        entity.workMode = request.workMode
        entity.jobType = request.jobType
        entity.salaryType = request.salaryType
        entity.salaryMin = request.salaryMin
        entity.salaryMax = request.salaryMax
        entity.salaryText = request.salaryText
        entity.currency = request.currency
        entity.salaryPeriod = request.salaryPeriod
        entity.closingDate = request.closingDate
        entity.updatedAt = Instant.now()

        return jobRepository.save(entity).toResponse(companyName, isOwner = true)
    }

    fun archive(id: UUID, userId: UUID) {
        val entity = jobRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Job not found")
        entity.archived = true
        entity.archivedAt = Instant.now()
        entity.updatedAt = Instant.now()
        jobRepository.save(entity)
    }

    fun setVisibility(id: UUID, visibility: Visibility, userId: UUID): JobResponse {
        val entity = jobRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Job not found")
        entity.visibility = visibility
        entity.updatedAt = Instant.now()
        val companyName = entity.companyId?.let { resolveCompanyName(it) }
        return jobRepository.save(entity).toResponse(companyName, isOwner = true)
    }

    @Transactional(readOnly = true)
    fun browsePublic(title: String?, pageable: Pageable): Page<BrowseJobResponse> {
        val page = jobRepository.findPublic(title, pageable)
        // Batch-fetch owner emails to avoid N+1
        val ownerIds = page.content.map { it.userId }.toSet()
        val ownerMap = if (ownerIds.isNotEmpty()) {
            userRepository.findAllById(ownerIds).associateBy({ it.id!! }, { it.email })
        } else {
            emptyMap()
        }
        // Batch-fetch company names
        val companyIds = page.content.mapNotNull { it.companyId }.toSet()
        val companyNameMap = if (companyIds.isNotEmpty()) {
            companyRepository.findAllById(companyIds).associateBy({ it.id!! }, { it.name })
        } else {
            emptyMap()
        }
        return page.map { entity ->
            BrowseJobResponse(
                id = entity.id!!,
                title = entity.title,
                description = entity.description,
                location = entity.location,
                workMode = entity.workMode?.name,
                jobType = entity.jobType?.name,
                companyName = entity.companyId?.let { companyNameMap[it] },
                ownerEmail = ownerMap[entity.userId] ?: "unknown",
                createdAt = entity.createdAt
            )
        }
    }

    @Transactional(readOnly = true)
    fun sharedWithMe(userId: UUID, pageable: Pageable): Page<JobResponse> {
        val page = jobRepository.findSharedWithUser(userId, pageable)
        val companyIds = page.content.mapNotNull { it.companyId }.toSet()
        val companyNameMap = if (companyIds.isNotEmpty()) {
            companyRepository.findAllById(companyIds).associateBy({ it.id!! }, { it.name })
        } else {
            emptyMap()
        }
        return page.map { it.toResponse(companyNameMap[it.companyId], isOwner = false) }
    }

    private fun validateCompany(companyId: UUID, userId: UUID): String {
        val company = companyRepository.findByIdAndUserIdAndArchivedFalse(companyId, userId)
            ?: throw NotFoundException("Company not found")
        return company.name
    }

    private fun resolveCompanyName(companyId: UUID): String? {
        return companyRepository.findById(companyId).orElse(null)?.name
    }

    private fun JobEntity.toResponse(companyName: String?, isOwner: Boolean = true): JobResponse = JobResponse(
        id = id!!,
        title = title,
        description = description,
        url = url,
        notes = notes,
        location = location,
        workMode = workMode,
        jobType = jobType,
        companyId = companyId,
        companyName = companyName,
        salaryType = salaryType,
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        salaryText = salaryText,
        currency = currency,
        salaryPeriod = salaryPeriod,
        closingDate = closingDate,
        visibility = visibility.name,
        archived = archived,
        archivedAt = archivedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isOwner = isOwner
    )
}
