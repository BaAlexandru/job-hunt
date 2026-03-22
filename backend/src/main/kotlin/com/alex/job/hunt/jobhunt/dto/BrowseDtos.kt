package com.alex.job.hunt.jobhunt.dto

import java.time.Instant
import java.util.UUID

data class BrowseCompanyResponse(
    val id: UUID,
    val name: String,
    val website: String?,
    val location: String?,
    val notes: String?,
    val ownerEmail: String,
    val createdAt: Instant
)

data class BrowseJobResponse(
    val id: UUID,
    val title: String,
    val description: String?,
    val location: String?,
    val workMode: String?,
    val jobType: String?,
    val companyName: String?,
    val ownerEmail: String,
    val createdAt: Instant
)
