package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.BrowseCompanyResponse
import com.alex.job.hunt.jobhunt.dto.BrowseJobResponse
import com.alex.job.hunt.jobhunt.service.CompanyService
import com.alex.job.hunt.jobhunt.service.JobService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/browse")
class BrowseController(
    private val companyService: CompanyService,
    private val jobService: JobService
) {

    @GetMapping("/companies")
    fun browseCompanies(
        @RequestParam(required = false) q: String?,
        pageable: Pageable
    ): ResponseEntity<Page<BrowseCompanyResponse>> {
        return ResponseEntity.ok(companyService.browsePublic(q, pageable))
    }

    @GetMapping("/jobs")
    fun browseJobs(
        @RequestParam(required = false) q: String?,
        pageable: Pageable
    ): ResponseEntity<Page<BrowseJobResponse>> {
        return ResponseEntity.ok(jobService.browsePublic(q, pageable))
    }
}
