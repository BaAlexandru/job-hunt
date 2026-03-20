package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.EmailVerificationToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface EmailVerificationTokenRepository : JpaRepository<EmailVerificationToken, UUID> {
    fun findByToken(token: String): EmailVerificationToken?
}
