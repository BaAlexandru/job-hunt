package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.CreateJobRequest
import com.alex.job.hunt.jobhunt.dto.CreateShareRequest
import com.alex.job.hunt.jobhunt.dto.JobResponse
import com.alex.job.hunt.jobhunt.dto.SetVisibilityRequest
import com.alex.job.hunt.jobhunt.dto.ShareResponse
import com.alex.job.hunt.jobhunt.dto.UpdateJobRequest
import com.alex.job.hunt.jobhunt.entity.JobType
import com.alex.job.hunt.jobhunt.entity.ResourceType
import com.alex.job.hunt.jobhunt.entity.WorkMode
import com.alex.job.hunt.jobhunt.security.SecurityContextUtil
import com.alex.job.hunt.jobhunt.service.JobService
import com.alex.job.hunt.jobhunt.service.ShareService
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
import java.util.UUID

@RestController
@RequestMapping("/api/jobs")
class JobController(
    private val jobService: JobService,
    private val shareService: ShareService
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateJobRequest): ResponseEntity<JobResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.create(request, userId))
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<JobResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(jobService.getById(id, userId))
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) companyId: UUID?,
        @RequestParam(required = false) jobType: JobType?,
        @RequestParam(required = false) workMode: WorkMode?,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
        pageable: Pageable
    ): ResponseEntity<Page<JobResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(jobService.list(userId, companyId, jobType, workMode, q, includeArchived, pageable))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateJobRequest
    ): ResponseEntity<JobResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(jobService.update(id, request, userId))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        jobService.archive(id, userId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/visibility")
    fun setVisibility(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SetVisibilityRequest
    ): ResponseEntity<JobResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(jobService.setVisibility(id, request.visibility, userId))
    }

    @GetMapping("/{id}/shares")
    fun listShares(@PathVariable id: UUID): ResponseEntity<List<ShareResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(shareService.listShares(ResourceType.JOB, id, userId))
    }

    @PostMapping("/{id}/shares")
    fun createShare(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateShareRequest
    ): ResponseEntity<ShareResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        val share = shareService.createShare(ResourceType.JOB, id, request.email, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(share)
    }

    @DeleteMapping("/{id}/shares/{shareId}")
    fun revokeShare(@PathVariable id: UUID, @PathVariable shareId: UUID): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        shareService.revokeShare(shareId, userId)
        return ResponseEntity.noContent().build()
    }
}
