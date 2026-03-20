package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.TimelineEntry
import com.alex.job.hunt.jobhunt.dto.TimelineEntryType
import com.alex.job.hunt.jobhunt.security.SecurityContextUtil
import com.alex.job.hunt.jobhunt.service.TimelineService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/applications/{applicationId}/timeline")
class TimelineController(private val timelineService: TimelineService) {

    @GetMapping
    fun getTimeline(
        @PathVariable applicationId: UUID,
        @RequestParam(required = false) types: List<TimelineEntryType>?
    ): ResponseEntity<List<TimelineEntry>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(timelineService.getTimeline(applicationId, userId, types))
    }
}
