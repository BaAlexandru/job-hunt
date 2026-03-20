package com.alex.job.hunt.jobhunt.config

import org.springframework.context.annotation.Configuration

@Configuration
class RedisConfig {
    // Spring Boot auto-configures StringRedisTemplate via spring-boot-starter-data-redis
    // This class exists as a placeholder for future Redis customization
    // No explicit beans needed -- Docker Compose integration provides connection details
}
