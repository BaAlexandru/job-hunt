package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.TimelineEntry
import com.alex.job.hunt.jobhunt.dto.TimelineEntryType
import com.alex.job.hunt.jobhunt.entity.ApplicationNoteEntity
import com.alex.job.hunt.jobhunt.entity.InterviewEntity
import com.alex.job.hunt.jobhunt.entity.InterviewNoteEntity
import com.alex.job.hunt.jobhunt.repository.ApplicationNoteRepository
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import com.alex.job.hunt.jobhunt.repository.InterviewNoteRepository
import com.alex.job.hunt.jobhunt.repository.InterviewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class TimelineService(
    private val interviewRepository: InterviewRepository,
    private val applicationNoteRepository: ApplicationNoteRepository,
    private val interviewNoteRepository: InterviewNoteRepository,
    private val applicationRepository: ApplicationRepository
) {

    fun getTimeline(applicationId: UUID, userId: UUID, types: List<TimelineEntryType>?): List<TimelineEntry> {
        applicationRepository.findByIdAndUserId(applicationId, userId)
            ?: throw NotFoundException("Application not found")

        val entries = mutableListOf<TimelineEntry>()

        if (types == null || TimelineEntryType.INTERVIEW in types) {
            entries += interviewRepository.findByApplicationIdAndArchivedFalse(applicationId)
                .map { it.toTimelineEntry() }
        }

        if (types == null || TimelineEntryType.APPLICATION_NOTE in types) {
            entries += applicationNoteRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId)
                .map { it.toTimelineEntry() }
        }

        if (types == null || TimelineEntryType.INTERVIEW_NOTE in types) {
            val interviewIds = interviewRepository.findIdsByApplicationId(applicationId)
            if (interviewIds.isNotEmpty()) {
                entries += interviewNoteRepository.findByInterviewIdIn(interviewIds)
                    .map { it.toTimelineEntry() }
            }
        }

        return entries.sortedByDescending { it.date }
    }

    private fun InterviewEntity.toTimelineEntry(): TimelineEntry = TimelineEntry(
        id = id!!,
        date = scheduledAt,
        type = TimelineEntryType.INTERVIEW,
        summary = "Round $roundNumber: $stage ($interviewType) - $outcome",
        details = mapOf(
            "roundNumber" to roundNumber,
            "stage" to stage.name,
            "interviewType" to interviewType.name,
            "outcome" to outcome.name,
            "result" to result.name,
            "location" to location,
            "interviewerNames" to interviewerNames
        )
    )

    private fun ApplicationNoteEntity.toTimelineEntry(): TimelineEntry = TimelineEntry(
        id = id!!,
        date = createdAt,
        type = TimelineEntryType.APPLICATION_NOTE,
        summary = content.take(200),
        details = mapOf("noteType" to noteType.name)
    )

    private fun InterviewNoteEntity.toTimelineEntry(): TimelineEntry = TimelineEntry(
        id = id!!,
        date = createdAt,
        type = TimelineEntryType.INTERVIEW_NOTE,
        summary = content.take(200),
        details = mapOf(
            "noteType" to noteType.name,
            "interviewId" to interviewId
        )
    )
}
