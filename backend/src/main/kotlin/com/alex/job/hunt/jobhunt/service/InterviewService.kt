package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.CreateInterviewRequest
import com.alex.job.hunt.jobhunt.dto.InterviewResponse
import com.alex.job.hunt.jobhunt.dto.UpdateInterviewRequest
import com.alex.job.hunt.jobhunt.entity.InterviewEntity
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import com.alex.job.hunt.jobhunt.repository.InterviewRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class InterviewService(
    private val interviewRepository: InterviewRepository,
    private val applicationRepository: ApplicationRepository
) {

    fun create(request: CreateInterviewRequest, userId: UUID): InterviewResponse {
        applicationRepository.findByIdAndUserId(request.applicationId, userId)
            ?: throw NotFoundException("Application not found")

        val nextRound = interviewRepository.findMaxRoundNumberByApplicationId(request.applicationId) + 1

        val entity = InterviewEntity(
            applicationId = request.applicationId,
            userId = userId,
            roundNumber = nextRound,
            scheduledAt = request.scheduledAt,
            durationMinutes = request.durationMinutes,
            interviewType = request.interviewType,
            stage = request.stage,
            stageLabel = request.stageLabel,
            location = request.location,
            interviewerNames = request.interviewerNames
        )
        return interviewRepository.save(entity).toResponse()
    }

    @Transactional(readOnly = true)
    fun get(id: UUID, userId: UUID): InterviewResponse {
        val entity = interviewRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Interview not found")
        return entity.toResponse()
    }

    @Transactional(readOnly = true)
    fun listByApplication(applicationId: UUID, userId: UUID, pageable: Pageable): Page<InterviewResponse> {
        applicationRepository.findByIdAndUserId(applicationId, userId)
            ?: throw NotFoundException("Application not found")
        return interviewRepository.findByApplicationIdAndUserIdAndArchivedFalseOrderByScheduledAtAsc(
            applicationId, userId, pageable
        ).map { it.toResponse() }
    }

    fun update(id: UUID, request: UpdateInterviewRequest, userId: UUID): InterviewResponse {
        val entity = interviewRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Interview not found")

        request.scheduledAt?.let { entity.scheduledAt = it }
        request.interviewType?.let { entity.interviewType = it }
        request.stage?.let { entity.stage = it }
        request.stageLabel?.let { entity.stageLabel = it }
        request.durationMinutes?.let { entity.durationMinutes = it }
        request.location?.let { entity.location = it }
        request.interviewerNames?.let { entity.interviewerNames = it }
        request.outcome?.let { entity.outcome = it }
        request.result?.let { entity.result = it }
        request.candidateFeedback?.let { entity.candidateFeedback = it }
        request.companyFeedback?.let { entity.companyFeedback = it }
        entity.updatedAt = Instant.now()

        return interviewRepository.save(entity).toResponse()
    }

    fun archive(id: UUID, userId: UUID) {
        val entity = interviewRepository.findByIdAndUserId(id, userId)
            ?: throw NotFoundException("Interview not found")
        entity.archived = true
        entity.archivedAt = Instant.now()
        entity.updatedAt = Instant.now()
        interviewRepository.save(entity)
    }

    private fun InterviewEntity.toResponse(): InterviewResponse = InterviewResponse(
        id = id!!,
        applicationId = applicationId,
        roundNumber = roundNumber,
        scheduledAt = scheduledAt,
        durationMinutes = durationMinutes,
        interviewType = interviewType,
        stage = stage,
        stageLabel = stageLabel,
        outcome = outcome,
        result = result,
        location = location,
        interviewerNames = interviewerNames,
        candidateFeedback = candidateFeedback,
        companyFeedback = companyFeedback,
        archived = archived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
