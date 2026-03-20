package com.alex.job.hunt.jobhunt.repository

import com.alex.job.hunt.jobhunt.entity.TokenBlocklistEntry
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface TokenBlocklistRepository : JpaRepository<TokenBlocklistEntry, UUID> {
    fun existsByTokenId(tokenId: String): Boolean
    fun deleteByExpiresAtBefore(cutoff: Instant): Int
}
