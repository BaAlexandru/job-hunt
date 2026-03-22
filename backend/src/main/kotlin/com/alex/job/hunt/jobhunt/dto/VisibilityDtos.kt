package com.alex.job.hunt.jobhunt.dto

import com.alex.job.hunt.jobhunt.entity.Visibility
import jakarta.validation.constraints.NotNull

data class SetVisibilityRequest(
    @field:NotNull(message = "Visibility is required")
    val visibility: Visibility
)
