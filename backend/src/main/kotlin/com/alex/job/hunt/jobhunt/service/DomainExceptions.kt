package com.alex.job.hunt.jobhunt.service

class NotFoundException(message: String) : RuntimeException(message)

class ConflictException(message: String) : RuntimeException(message)
