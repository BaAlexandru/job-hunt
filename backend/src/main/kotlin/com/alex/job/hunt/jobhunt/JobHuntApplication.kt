package com.alex.job.hunt.jobhunt

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class JobHuntApplication

fun main(args: Array<String>) {
    runApplication<JobHuntApplication>(*args)
}
