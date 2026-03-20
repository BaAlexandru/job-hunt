package com.alex.job.hunt.jobhunt.security

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-expiration-ms}") private val accessExpirationMs: Long,
    @Value("\${jwt.refresh-expiration-ms}") private val refreshExpirationMs: Long
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))
    }

    fun createAccessToken(userId: UUID, email: String, role: String): String {
        val now = Date()
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(email)
            .claim("userId", userId.toString())
            .claim("role", role)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(Date(now.time + accessExpirationMs))
            .signWith(key)
            .compact()
    }

    fun createRefreshToken(userId: UUID, email: String): String {
        val now = Date()
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(email)
            .claim("userId", userId.toString())
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(Date(now.time + refreshExpirationMs))
            .signWith(key)
            .compact()
    }

    fun validateToken(token: String): Boolean = try {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
        true
    } catch (e: JwtException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    fun getUsername(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject

    fun getTokenId(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.id

    fun getTokenType(token: String): String =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload["type"] as String

    fun getExpiration(token: String): Date =
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.expiration

    fun getAccessExpirationMs(): Long = accessExpirationMs

    fun getRefreshExpirationMs(): Long = refreshExpirationMs
}
