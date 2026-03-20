package com.alex.job.hunt.jobhunt.config

import com.alex.job.hunt.jobhunt.dto.MessageResponse
import com.alex.job.hunt.jobhunt.service.AuthenticationException
import com.alex.job.hunt.jobhunt.service.InvalidTokenException
import com.alex.job.hunt.jobhunt.service.RateLimitException
import com.alex.job.hunt.jobhunt.service.RegistrationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class AuthExceptionHandler {

    @ExceptionHandler(RegistrationException::class)
    fun handleRegistrationException(ex: RegistrationException): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(MessageResponse(ex.message ?: "Registration failed"))

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse(ex.message ?: "Authentication failed"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("status" to 400, "errors" to errors))
    }

    @ExceptionHandler(MissingRequestCookieException::class)
    fun handleMissingCookie(ex: MissingRequestCookieException): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(MessageResponse("Refresh token required"))

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidTokenException(ex: InvalidTokenException): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(MessageResponse(ex.message ?: "Invalid token"))

    @ExceptionHandler(RateLimitException::class)
    fun handleRateLimitException(ex: RateLimitException): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(MessageResponse(ex.message ?: "Too many requests"))
}
