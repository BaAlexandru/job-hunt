package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.ApplicationResponse
import com.alex.job.hunt.jobhunt.dto.CreateApplicationRequest
import com.alex.job.hunt.jobhunt.dto.UpdateApplicationRequest
import com.alex.job.hunt.jobhunt.dto.UpdateStatusRequest
import com.alex.job.hunt.jobhunt.entity.ApplicationStatus
import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.NoteType
import com.alex.job.hunt.jobhunt.entity.WorkMode
import com.alex.job.hunt.jobhunt.security.SecurityContextUtil
import com.alex.job.hunt.jobhunt.service.ApplicationService
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/applications")
class ApplicationController(private val applicationService: ApplicationService) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateApplicationRequest): ResponseEntity<ApplicationResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.create(request, userId))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<ApplicationResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(applicationService.getById(id, userId))
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) status: List<ApplicationStatus>?,
        @RequestParam(required = false) companyId: UUID?,
        @RequestParam(required = false) jobType: JobType?,
        @RequestParam(required = false) workMode: WorkMode?,
        @RequestParam(required = false) dateFrom: LocalDate?,
        @RequestParam(required = false) dateTo: LocalDate?,
        @RequestParam(required = false) hasNextAction: Boolean?,
        @RequestParam(required = false) noteType: NoteType?,
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
        pageable: Pageable
    ): ResponseEntity<Page<ApplicationResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(
            applicationService.listFiltered(
                userId, status, companyId, jobType, workMode, q,
                dateFrom, dateTo, hasNextAction, noteType,
                includeArchived, pageable
            )
        )
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateApplicationRequest
    ): ResponseEntity<ApplicationResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(applicationService.update(id, request, userId))
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateStatusRequest
    ): ResponseEntity<ApplicationResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(applicationService.updateStatus(id, request, userId))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        applicationService.archive(id, userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}/transitions")
    fun getTransitions(@PathVariable id: UUID): ResponseEntity<Set<ApplicationStatus>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(applicationService.getValidTransitions(id, userId))
    }
}
