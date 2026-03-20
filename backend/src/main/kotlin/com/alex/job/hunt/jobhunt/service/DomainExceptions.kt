package com.alex.job.hunt.jobhunt.service

class NotFoundException(message: String) : RuntimeException(message)

class ConflictException(message: String) : RuntimeException(message)

class InvalidTransitionException(message: String) : RuntimeException(message)

class StorageException(message: String) : RuntimeException(message)

class InvalidFileTypeException(message: String) : RuntimeException(message)
