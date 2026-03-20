package com.alex.job.hunt.jobhunt.dto

import java.time.Instant

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: Instant = Instant.now(),
    val fieldErrors: Map<String, String>? = null
)
