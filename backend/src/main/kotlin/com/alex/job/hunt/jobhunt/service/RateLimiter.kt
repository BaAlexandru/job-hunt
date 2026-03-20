package com.alex.job.hunt.jobhunt.service

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RateLimiter(
    private val redisTemplate: StringRedisTemplate
) {

    fun isAllowed(key: String, maxRequests: Int, windowSeconds: Long): Boolean {
        val ops = redisTemplate.opsForValue()
        val current = ops.increment(key) ?: 1
        if (current == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds))
        }
        return current <= maxRequests
    }
}
