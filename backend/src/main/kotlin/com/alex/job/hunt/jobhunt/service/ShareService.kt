package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.ShareResponse
import com.alex.job.hunt.jobhunt.entity.ResourceShareEntity
import com.alex.job.hunt.jobhunt.entity.ResourceType
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.JobRepository
import com.alex.job.hunt.jobhunt.repository.ResourceShareRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ShareService(
    private val resourceShareRepository: ResourceShareRepository,
    private val companyRepository: CompanyRepository,
    private val jobRepository: JobRepository,
    private val userRepository: UserRepository
) {

    fun createShare(resourceType: ResourceType, resourceId: UUID, email: String, ownerId: UUID): ShareResponse {
        // Verify ownership
        verifyOwnership(resourceType, resourceId, ownerId)

        // Look up target user
        val targetUser = userRepository.findByEmail(email)
            ?: throw NotFoundException("User not found")

        // Self-share check
        if (targetUser.id == ownerId) {
            throw ConflictException("Cannot share with yourself")
        }

        // Duplicate check
        if (resourceShareRepository.existsByResourceTypeAndResourceIdAndSharedWithId(resourceType, resourceId, targetUser.id!!)) {
            throw ConflictException("Already shared with this user")
        }

        // Save
        val share = ResourceShareEntity(
            resourceType = resourceType,
            resourceId = resourceId,
            ownerId = ownerId,
            sharedWithId = targetUser.id!!
        )
        val saved = resourceShareRepository.save(share)

        return ShareResponse(
            id = saved.id!!,
            email = email,
            sharedAt = saved.createdAt
        )
    }

    fun revokeShare(shareId: UUID, ownerId: UUID) {
        val share = resourceShareRepository.findById(shareId)
            .orElseThrow { NotFoundException("Share not found") }

        if (share.ownerId != ownerId) {
            throw NotFoundException("Share not found")
        }

        resourceShareRepository.delete(share)
    }

    @Transactional(readOnly = true)
    fun listShares(resourceType: ResourceType, resourceId: UUID, ownerId: UUID): List<ShareResponse> {
        // Verify ownership
        verifyOwnership(resourceType, resourceId, ownerId)

        val shares = resourceShareRepository.findByResourceTypeAndResourceId(resourceType, resourceId)

        // Batch-fetch emails
        val sharedWithIds = shares.map { it.sharedWithId }.toSet()
        val emailMap = if (sharedWithIds.isNotEmpty()) {
            userRepository.findAllById(sharedWithIds).associateBy({ it.id!! }, { it.email })
        } else {
            emptyMap()
        }

        return shares.map { share ->
            ShareResponse(
                id = share.id!!,
                email = emailMap[share.sharedWithId] ?: "unknown",
                sharedAt = share.createdAt
            )
        }
    }

    private fun verifyOwnership(resourceType: ResourceType, resourceId: UUID, ownerId: UUID) {
        when (resourceType) {
            ResourceType.COMPANY -> companyRepository.findByIdAndUserId(resourceId, ownerId)
            ResourceType.JOB -> jobRepository.findByIdAndUserId(resourceId, ownerId)
        } ?: throw NotFoundException("Resource not found")
    }
}
