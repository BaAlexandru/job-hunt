package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.BrowseCompanyResponse
import com.alex.job.hunt.jobhunt.dto.CompanyResponse
import com.alex.job.hunt.jobhunt.dto.CreateCompanyRequest
import com.alex.job.hunt.jobhunt.dto.UpdateCompanyRequest
import com.alex.job.hunt.jobhunt.entity.CompanyEntity
import com.alex.job.hunt.jobhunt.entity.ResourceType
import com.alex.job.hunt.jobhunt.entity.Visibility
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
class CompanyService(
    private val companyRepository: CompanyRepository,
    private val jobRepository: JobRepository,
    private val userRepository: UserRepository,
    private val resourceShareRepository: ResourceShareRepository
) {

    fun create(request: CreateCompanyRequest, userId: UUID): CompanyResponse {
        val entity = CompanyEntity(
            userId = userId,
            name = request.name,
            website = request.website,
            location = request.location,
            notes = request.notes
        )
        return companyRepository.save(entity).toResponse(isOwner = true)
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID, userId: UUID): CompanyResponse {
        val entity = companyRepository.findByIdWithVisibility(id, userId)
            ?: throw NotFoundException("Company not found")
        val isOwner = entity.userId == userId
        return entity.toResponse(isOwner = isOwner)
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID, name: String?, includeArchived: Boolean, pageable: Pageable): Page<CompanyResponse> {
        return companyRepository.findFiltered(userId, name, includeArchived, pageable)
            .map { it.toResponse(isOwner = true) }
    }

    fun update(id: UUID, request: UpdateCompanyRequest, userId: UUID): CompanyResponse {
        val entity = companyRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Company not found")
        entity.name = request.name
        entity.website = request.website
        entity.location = request.location
        entity.notes = request.notes
        entity.updatedAt = Instant.now()
        return companyRepository.save(entity).toResponse(isOwner = true)
    }

    fun archive(id: UUID, userId: UUID) {
        val entity = companyRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Company not found")
        if (jobRepository.existsByCompanyIdAndUserIdAndArchivedFalse(id, userId)) {
            throw ConflictException("Cannot archive company with active job postings")
        }
        entity.archived = true
        entity.archivedAt = Instant.now()
        entity.updatedAt = Instant.now()
        companyRepository.save(entity)
    }

    fun setVisibility(id: UUID, visibility: Visibility, userId: UUID): CompanyResponse {
        val entity = companyRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Company not found")
        entity.visibility = visibility
        entity.updatedAt = Instant.now()
        return companyRepository.save(entity).toResponse(isOwner = true)
    }

    @Transactional(readOnly = true)
    fun browsePublic(name: String?, pageable: Pageable): Page<BrowseCompanyResponse> {
        val page = companyRepository.findPublic(name, pageable)
        // Batch-fetch owner emails to avoid N+1
        val ownerIds = page.content.map { it.userId }.toSet()
        val ownerMap = if (ownerIds.isNotEmpty()) {
            userRepository.findAllById(ownerIds).associateBy({ it.id!! }, { it.email })
        } else {
            emptyMap()
        }
        return page.map { entity ->
            BrowseCompanyResponse(
                id = entity.id!!,
                name = entity.name,
                website = entity.website,
                location = entity.location,
                notes = entity.notes,
                ownerEmail = ownerMap[entity.userId] ?: "unknown",
                createdAt = entity.createdAt
            )
        }
    }

    @Transactional(readOnly = true)
    fun sharedWithMe(userId: UUID, pageable: Pageable): Page<CompanyResponse> {
        return companyRepository.findSharedWithUser(userId, pageable)
            .map { it.toResponse(isOwner = false) }
    }

    private fun CompanyEntity.toResponse(isOwner: Boolean = true): CompanyResponse = CompanyResponse(
        id = id!!,
        name = name,
        website = website,
        location = location,
        notes = notes,
        visibility = visibility.name,
        archived = archived,
        archivedAt = archivedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isOwner = isOwner
    )
}
