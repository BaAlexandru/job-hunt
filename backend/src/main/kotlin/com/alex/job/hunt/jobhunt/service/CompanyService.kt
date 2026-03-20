package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.CompanyResponse
import com.alex.job.hunt.jobhunt.dto.CreateCompanyRequest
import com.alex.job.hunt.jobhunt.dto.UpdateCompanyRequest
import com.alex.job.hunt.jobhunt.entity.CompanyEntity
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CompanyService(private val companyRepository: CompanyRepository) {

    fun create(request: CreateCompanyRequest, userId: UUID): CompanyResponse {
        val entity = CompanyEntity(
            userId = userId,
            name = request.name,
            website = request.website,
            location = request.location,
            notes = request.notes
        )
        return companyRepository.save(entity).toResponse()
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID, userId: UUID): CompanyResponse {
        val entity = companyRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Company not found")
        return entity.toResponse()
    }

    @Transactional(readOnly = true)
    fun list(userId: UUID, name: String?, includeArchived: Boolean, pageable: Pageable): Page<CompanyResponse> {
        return companyRepository.findFiltered(userId, name, includeArchived, pageable)
            .map { it.toResponse() }
    }

    fun update(id: UUID, request: UpdateCompanyRequest, userId: UUID): CompanyResponse {
        val entity = companyRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Company not found")
        entity.name = request.name
        entity.website = request.website
        entity.location = request.location
        entity.notes = request.notes
        entity.updatedAt = Instant.now()
        return companyRepository.save(entity).toResponse()
    }

    fun archive(id: UUID, userId: UUID) {
        val entity = companyRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Company not found")
        entity.archived = true
        entity.archivedAt = Instant.now()
        entity.updatedAt = Instant.now()
        companyRepository.save(entity)
    }

    private fun CompanyEntity.toResponse(): CompanyResponse = CompanyResponse(
        id = id!!,
        name = name,
        website = website,
        location = location,
        notes = notes,
        archived = archived,
        archivedAt = archivedAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
