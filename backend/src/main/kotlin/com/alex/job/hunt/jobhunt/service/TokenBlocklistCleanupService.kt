package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.repository.TokenBlocklistRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class TokenBlocklistCleanupService(
    private val tokenBlocklistRepository: TokenBlocklistRepository
) {

    private val logger = LoggerFactory.getLogger(TokenBlocklistCleanupService::class.java)

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    fun cleanupExpiredTokens() {
        val deleted = tokenBlocklistRepository.deleteByExpiresAtBefore(Instant.now())
        logger.info("Cleaned up $deleted expired blocklist entries")
    }
}
