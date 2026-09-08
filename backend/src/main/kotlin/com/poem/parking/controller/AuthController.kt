package com.poem.parking.controller

import com.poem.parking.config.CurrentUser
import com.poem.parking.dto.*
import com.poem.parking.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val currentUser: CurrentUser,
) {
    /** 휴대폰 인증번호 요청 (목업) */
    @PostMapping("/otp")
    fun requestOtp(@Valid @RequestBody req: OtpRequest): ResponseEntity<Map<String, String>> {
        authService.requestOtp(req)
        return ResponseEntity.ok(mapOf("message" to "인증번호가 발송되었습니다."))
    }

    /** 인증번호 확인 + 토큰 발급 */
    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): TokenResponse = authService.login(req)

    /** 아파트 목록 (인증 화면 선택용) */
    @GetMapping("/apartments")
    fun apartments(): List<ApartmentSummary> = authService.listApartments()
}

@RestController
@RequestMapping("/api/v1/me")
class MeController(
    private val authService: AuthService,
    private val currentUser: CurrentUser,
) {
    @GetMapping
    fun me(): UserResponse = authService.me(currentUser.user())

    /** 아파트/세대 인증 (목업) */
    @PostMapping("/verify-apartment")
    fun verify(@Valid @RequestBody req: ApartmentVerifyRequest): UserResponse =
        authService.verifyApartment(currentUser.user(), req)
}
