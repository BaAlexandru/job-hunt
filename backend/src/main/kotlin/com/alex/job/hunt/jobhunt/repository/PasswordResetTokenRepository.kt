package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, UUID> {
    fun findByToken(token: String): PasswordResetToken?
}
