package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.CreateJobRequest
import com.alex.job.hunt.jobhunt.dto.JobResponse
import com.alex.job.hunt.jobhunt.dto.UpdateJobRequest
import com.alex.job.hunt.jobhunt.entity.JobEntity
import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.WorkMode
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.JobRepository
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
    private val companyRepository: CompanyRepository
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
        return jobRepository.save(entity).toResponse(companyName)
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID, userId: UUID): JobResponse {
        val entity = jobRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Job not found")
        val companyName = entity.companyId?.let { resolveCompanyName(it, userId) }
        return entity.toResponse(companyName)
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
        return page.map { it.toResponse(companyNameMap[it.companyId]) }
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

        return jobRepository.save(entity).toResponse(companyName)
    }

    fun archive(id: UUID, userId: UUID) {
        val entity = jobRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Job not found")
        entity.archived = true
        entity.archivedAt = Instant.now()
        entity.updatedAt = Instant.now()
        jobRepository.save(entity)
    }

    private fun validateCompany(companyId: UUID, userId: UUID): String {
        val company = companyRepository.findByIdAndUserIdAndArchivedFalse(companyId, userId)
            ?: throw NotFoundException("Company not found")
        return company.name
    }

    private fun resolveCompanyName(companyId: UUID, userId: UUID): String? {
        return companyRepository.findByIdAndUserId(companyId, userId)?.name
    }

    private fun JobEntity.toResponse(companyName: String?): JobResponse = JobResponse(
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
        updatedAt = updatedAt
    )
}
