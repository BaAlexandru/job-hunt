package com.alex.job.hunt.jobhunt.security

import org.springframework.security.core.context.SecurityContextHolder
import java.util.UUID

object SecurityContextUtil {

    fun getCurrentUserId(): UUID {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("No authenticated user in SecurityContext")
        val principal = authentication.principal as? AppUserDetails
            ?: throw IllegalStateException("Expected AppUserDetails but got ${authentication.principal!!::class}")
        return principal.getUserId()
    }
}
