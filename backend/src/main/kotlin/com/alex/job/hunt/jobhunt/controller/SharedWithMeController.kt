package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.CompanyResponse
import com.alex.job.hunt.jobhunt.dto.JobResponse
import com.alex.job.hunt.jobhunt.security.SecurityContextUtil
import com.alex.job.hunt.jobhunt.service.CompanyService
import com.alex.job.hunt.jobhunt.service.JobService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/shared")
class SharedWithMeController(
    private val companyService: CompanyService,
    private val jobService: JobService
) {

    @GetMapping("/companies")
    fun sharedCompanies(pageable: Pageable): ResponseEntity<Page<CompanyResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(companyService.sharedWithMe(userId, pageable))
    }

    @GetMapping("/jobs")
    fun sharedJobs(pageable: Pageable): ResponseEntity<Page<JobResponse>> {
        val userId = SecurityContextUtil.getCurrentUserId()
        return ResponseEntity.ok(jobService.sharedWithMe(userId, pageable))
    }
}
