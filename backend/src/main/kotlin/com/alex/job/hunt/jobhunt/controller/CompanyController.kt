package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.CompanyResponse
import com.alex.job.hunt.jobhunt.dto.CreateCompanyRequest
import com.alex.job.hunt.jobhunt.dto.CreateShareRequest
import com.alex.job.hunt.jobhunt.dto.SetVisibilityRequest
import com.alex.job.hunt.jobhunt.dto.ShareResponse
import com.alex.job.hunt.jobhunt.dto.UpdateCompanyRequest
import com.alex.job.hunt.jobhunt.entity.ResourceType
import com.alex.job.hunt.jobhunt.security.SecurityContextUtil
import com.alex.job.hunt.jobhunt.service.CompanyService
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
@RequestMapping("/api/companies")
class CompanyController(
    private val companyService: CompanyService,
    private val shareService: ShareService
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateCompanyRequest): ResponseEntity<CompanyResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        val company = companyService.create(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(company)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<CompanyResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(companyService.getById(id, userId))
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "false") includeArchived: Boolean,
        pageable: Pageable
    ): ResponseEntity<Page<CompanyResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(companyService.list(userId, q, includeArchived, pageable))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCompanyRequest
    ): ResponseEntity<CompanyResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(companyService.update(id, request, userId))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        companyService.archive(id, userId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/visibility")
    fun setVisibility(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SetVisibilityRequest
    ): ResponseEntity<CompanyResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(companyService.setVisibility(id, request.visibility, userId))
    }

    @GetMapping("/{id}/shares")
    fun listShares(@PathVariable id: UUID): ResponseEntity<List<ShareResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(shareService.listShares(ResourceType.COMPANY, id, userId))
    }

    @PostMapping("/{id}/shares")
    fun createShare(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateShareRequest
    ): ResponseEntity<ShareResponse> {
        val userId = SecurityContextUtil.getCurrentUserId()
        val share = shareService.createShare(ResourceType.COMPANY, id, request.email, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(share)
    }

    @DeleteMapping("/{id}/shares/{shareId}")
    fun revokeShare(@PathVariable id: UUID, @PathVariable shareId: UUID): ResponseEntity<Void> {
        val userId = SecurityContextUtil.getCurrentUserId()
        shareService.revokeShare(shareId, userId)
        return ResponseEntity.noContent().build()
    }
}
