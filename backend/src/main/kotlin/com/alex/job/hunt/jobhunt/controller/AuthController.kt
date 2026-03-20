package com.alex.job.hunt.jobhunt.controller

import com.alex.job.hunt.jobhunt.dto.AuthRequest
import com.alex.job.hunt.jobhunt.dto.AuthResponse
import com.alex.job.hunt.jobhunt.dto.MessageResponse
import com.alex.job.hunt.jobhunt.dto.PasswordResetConfirmRequest
import com.alex.job.hunt.jobhunt.dto.PasswordResetRequest
import com.alex.job.hunt.jobhunt.dto.RegisterRequest
import com.alex.job.hunt.jobhunt.security.JwtTokenProvider
import com.alex.job.hunt.jobhunt.service.AuthService
import com.alex.job.hunt.jobhunt.service.EmailVerificationService
import com.alex.job.hunt.jobhunt.service.PasswordResetService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val emailVerificationService: EmailVerificationService,
    private val passwordResetService: PasswordResetService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<MessageResponse> {
        val result = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(result)
    }

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AuthRequest,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val (authResponse, refreshToken) = authService.login(request)

        val cookie = Cookie("refresh_token", refreshToken)
        cookie.isHttpOnly = true
        cookie.secure = false
        cookie.path = "/api/auth/refresh"
        cookie.maxAge = (jwtTokenProvider.getRefreshExpirationMs() / 1000).toInt()
        cookie.setAttribute("SameSite", "Lax")
        response.addCookie(cookie)

        return ResponseEntity.ok(authResponse)
    }

    @PostMapping("/refresh")
    fun refresh(
        @CookieValue("refresh_token") refreshToken: String,
        response: HttpServletResponse
    ): ResponseEntity<AuthResponse> {
        val (authResponse, newRefreshToken) = authService.refresh(refreshToken)

        val cookie = Cookie("refresh_token", newRefreshToken)
        cookie.isHttpOnly = true
        cookie.secure = false
        cookie.path = "/api/auth/refresh"
        cookie.maxAge = (jwtTokenProvider.getRefreshExpirationMs() / 1000).toInt()
        cookie.setAttribute("SameSite", "Lax")
        response.addCookie(cookie)

        return ResponseEntity.ok(authResponse)
    }

    @PostMapping("/logout")
    fun logout(
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<MessageResponse> {
        val accessToken = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)

        val refreshToken = request.cookies
            ?.firstOrNull { it.name == "refresh_token" }
            ?.value

        authService.logout(accessToken, refreshToken)

        // Clear the refresh cookie
        val cookie = Cookie("refresh_token", "")
        cookie.isHttpOnly = true
        cookie.secure = false
        cookie.path = "/api/auth/refresh"
        cookie.maxAge = 0
        cookie.setAttribute("SameSite", "Lax")
        response.addCookie(cookie)

        return ResponseEntity.ok(MessageResponse("Logged out successfully"))
    }

    @GetMapping("/verify")
    fun verifyEmail(@RequestParam token: String): ResponseEntity<MessageResponse> {
        val result = emailVerificationService.verify(token)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/password-reset")
    fun requestPasswordReset(@Valid @RequestBody request: PasswordResetRequest): ResponseEntity<MessageResponse> {
        val result = passwordResetService.requestReset(request.email)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/password-reset/confirm")
    fun confirmPasswordReset(@Valid @RequestBody request: PasswordResetConfirmRequest): ResponseEntity<MessageResponse> {
        val result = passwordResetService.confirmReset(request.token, request.newPassword)
        return ResponseEntity.ok(result)
    }
}
