package com.alex.job.hunt.jobhunt.dto

data class AuthResponse(
    val accessToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer"
)
