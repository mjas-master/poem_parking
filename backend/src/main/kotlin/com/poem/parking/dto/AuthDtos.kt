package com.poem.parking.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/** 1단계: 휴대폰 OTP 요청 (목업: 항상 성공, 코드는 설정값) */
data class OtpRequest(
    val phone: String,
)

/** 2단계: OTP 검증 → JWT 발급 */
data class LoginRequest(
    val phone: String,
    val otp: String? = null,
    val name: String? = null,
)

data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: UserResponse,
)

/** 아파트/세대 인증 (목업) */
data class ApartmentVerifyRequest(
    val apartmentCode: String,
    val dong: String? = null,
    val ho: String? = null,
    /** 관리사무소 발급 인증코드 (목업: 000000) */
    val verifyCode: String? = null,
)

data class UserResponse(
    val id: Long,
    val phone: String,
    val name: String,
    val verified: Boolean,
    val apartment: ApartmentSummary?,
    val dong: String?,
    val ho: String?,
)

data class ApartmentSummary(
    val code: String,
    val name: String,
    val freeMinutes: Int,
    val unitMinutes: Int,
    val unitFee: Int,
    val dailyCapFee: Int,
)
