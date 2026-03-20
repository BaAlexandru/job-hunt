package com.alex.job.hunt.jobhunt.dto

import java.time.Instant
import java.util.UUID

enum class TimelineEntryType { INTERVIEW, APPLICATION_NOTE, INTERVIEW_NOTE }

data class TimelineEntry(
    val id: UUID,
    val date: Instant,
    val type: TimelineEntryType,
    val summary: String,
    val details: Map<String, Any?>? = null
)
