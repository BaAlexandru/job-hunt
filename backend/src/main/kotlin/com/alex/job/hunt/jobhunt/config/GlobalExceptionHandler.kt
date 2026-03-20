package com.alex.job.hunt.jobhunt.config

import com.alex.job.hunt.jobhunt.dto.ErrorResponse
import com.alex.job.hunt.jobhunt.service.AuthenticationException
import com.alex.job.hunt.jobhunt.service.ConflictException
import com.alex.job.hunt.jobhunt.service.InvalidTokenException
import com.alex.job.hunt.jobhunt.service.InvalidTransitionException
import com.alex.job.hunt.jobhunt.service.NotFoundException
import com.alex.job.hunt.jobhunt.service.RateLimitException
import com.alex.job.hunt.jobhunt.service.RegistrationException
import com.alex.job.hunt.jobhunt.service.StorageException
import com.alex.job.hunt.jobhunt.service.InvalidFileTypeException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.http.converter.HttpMessageNotReadableException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(RegistrationException::class)
    fun handleRegistrationException(
        ex: RegistrationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Registration failed", request)

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(
        ex: AuthenticationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.UNAUTHORIZED, ex.message ?: "Authentication failed", request)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors)
    }

    @ExceptionHandler(MissingRequestCookieException::class)
    fun handleMissingCookie(
        ex: MissingRequestCookieException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.UNAUTHORIZED, "Refresh token required", request)

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidTokenException(
        ex: InvalidTokenException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid token", request)

    @ExceptionHandler(RateLimitException::class)
    fun handleRateLimitException(
        ex: RateLimitException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.message ?: "Too many requests", request)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMessageNotReadable(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request)

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(
        ex: NotFoundException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.NOT_FOUND, ex.message ?: "Resource not found", request)

    @ExceptionHandler(InvalidTransitionException::class)
    fun handleInvalidTransitionException(
        ex: InvalidTransitionException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.message ?: "Invalid status transition", request)

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(
        ex: ConflictException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.CONFLICT, ex.message ?: "Conflict", request)

    @ExceptionHandler(StorageException::class)
    fun handleStorageException(
        ex: StorageException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.message ?: "Storage operation failed", request)

    @ExceptionHandler(InvalidFileTypeException::class)
    fun handleInvalidFileTypeException(
        ex: InvalidFileTypeException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid file type", request)

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(
        ex: MaxUploadSizeExceededException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> =
        buildResponse(HttpStatus.BAD_REQUEST, "File size exceeds maximum allowed size of 25MB", request)

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception at ${request.requestURI}", ex)
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request)
    }

    private fun buildResponse(
        status: HttpStatus,
        message: String,
        request: HttpServletRequest,
        fieldErrors: Map<String, String>? = null
    ): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(
                status = status.value(),
                error = status.reasonPhrase,
                message = message,
                path = request.requestURI,
                fieldErrors = fieldErrors
            )
        )
}
