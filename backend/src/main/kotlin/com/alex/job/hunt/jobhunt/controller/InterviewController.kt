package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.CreateInterviewRequest
import com.alex.job.hunt.jobhunt.dto.InterviewResponse
import com.alex.job.hunt.jobhunt.dto.UpdateInterviewRequest
import com.alex.job.hunt.jobhunt.security.SecurityContextUtil
import com.alex.job.hunt.jobhunt.service.InterviewService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/interviews")
class InterviewController(private val interviewService: InterviewService) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateInterviewRequest): ResponseEntity<InterviewResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(interviewService.create(request, userId))
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ResponseEntity<InterviewResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(interviewService.get(id, userId))
    }

    @GetMapping
    fun listByApplication(@RequestParam applicationId: UUID, pageable: Pageable): ResponseEntity<Page<InterviewResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(interviewService.listByApplication(applicationId, userId, pageable))
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: UpdateInterviewRequest): ResponseEntity<InterviewResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(interviewService.update(id, request, userId))
    }

    @DeleteMapping("/{id}")
    fun archive(@PathVariable id: UUID): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        interviewService.archive(id, userId)
        return ResponseEntity.noContent().build()
    }
}
