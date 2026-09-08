package com.poem.parking.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date

@Component
class JwtProvider(private val props: AppProperties) {
    private val key = Keys.hmacShaKeyFor(props.jwt.secret.toByteArray(StandardCharsets.UTF_8))

    val expirationSeconds: Long get() = props.jwt.expirationMinutes * 60

    fun generate(userId: Long, phone: String): String {
        val now = Date()
        val exp = Date(now.time + props.jwt.expirationMinutes * 60_000)
        return Jwts.builder()
            .subject(userId.toString())
            .claim("phone", phone)
            .issuedAt(now)
            .expiration(exp)
            .signWith(key)
            .compact()
    }

    fun parse(token: String): Claims? = try {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
    } catch (e: Exception) {
        null
    }
}
