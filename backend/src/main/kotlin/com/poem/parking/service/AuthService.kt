package com.poem.parking.service

import com.poem.parking.config.AppProperties
import com.poem.parking.config.JwtProvider
import com.poem.parking.domain.Apartment
import com.poem.parking.domain.Household
import com.poem.parking.domain.User
import com.poem.parking.dto.*
import com.poem.parking.exception.BadRequestException
import com.poem.parking.exception.NotFoundException
import com.poem.parking.repository.ApartmentRepository
import com.poem.parking.repository.HouseholdRepository
import com.poem.parking.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class AuthService(
    private val userRepository: UserRepository,
    private val apartmentRepository: ApartmentRepository,
    private val householdRepository: HouseholdRepository,
    private val jwtProvider: JwtProvider,
    private val props: AppProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** [MOCK] SMS OTP 발송. 실제로는 SMS 게이트웨이 호출 + OTP 저장(TTL) */
    fun requestOtp(req: OtpRequest) {
        log.info("[MOCK SMS] to={} otp={}", req.phone, props.mock.smsOtp)
    }

    /** [MOCK] OTP 검증 후 로그인/회원가입 + JWT 발급 */
    fun login(req: LoginRequest): TokenResponse {
        val phone = req.phone.trim().ifBlank { "01012345678" }
        val user = userRepository.findByPhone(phone)
            ?: userRepository.save(User(phone = phone, name = req.name?.takeIf { it.isNotBlank() } ?: "입주민"))
        if (!req.name.isNullOrBlank()) user.name = req.name
        
        // --- 테스트용 자동 인증 및 동호수 세팅 ---
        if (!user.verified) {
            val apt = apartmentRepository.findByCode("APT-0001")
                ?: apartmentRepository.findAll().firstOrNull()
                ?: apartmentRepository.save(Apartment(code = "APT-0001", name = "테스트 아파트"))
            val household = householdRepository.findByApartmentAndDongAndHo(apt, "101", "101")
                ?: householdRepository.save(Household(apt, "101", "101"))
            user.household = household
            user.verified = true
        }

        return TokenResponse(
            accessToken = jwtProvider.generate(user.id!!, user.phone),
            expiresInSeconds = jwtProvider.expirationSeconds,
            user = toUserResponse(user),
        )
    }

    /**
     * [MOCK] 아파트/세대 인증.
     * 실제 서비스에서는 관리사무소 발급 인증코드, 세대 대표 승인, 고지서 QR 등으로 대체.
     */
    fun verifyApartment(user: User, req: ApartmentVerifyRequest): UserResponse {
        val apt = apartmentRepository.findByCode(req.apartmentCode.trim().uppercase())
            ?: apartmentRepository.findAll().firstOrNull()
            ?: apartmentRepository.save(Apartment(code = req.apartmentCode.trim().uppercase(), name = req.apartmentCode.trim()))
        val dong = req.dong?.trim()?.ifBlank { "101" } ?: "101"
        val ho = req.ho?.trim()?.ifBlank { "101" } ?: "101"
        val household = householdRepository.findByApartmentAndDongAndHo(apt, dong, ho)
            ?: householdRepository.save(Household(apt, dong, ho))
        user.household = household
        user.verified = true
        return toUserResponse(user)
    }

    fun me(user: User) = toUserResponse(user)

    fun listApartments(): List<ApartmentSummary> = apartmentRepository.findAll().map { toAptSummary(it) }

    fun toUserResponse(u: User): UserResponse {
        val hh = u.household
        return UserResponse(
            id = u.id!!, phone = u.phone, name = u.name, verified = u.verified,
            apartment = hh?.apartment?.let { toAptSummary(it) },
            dong = hh?.dong, ho = hh?.ho,
        )
    }

    private fun toAptSummary(a: com.poem.parking.domain.Apartment) =
        ApartmentSummary(a.code, a.name, a.freeMinutes, a.unitMinutes, a.unitFee, a.dailyCapFee)
}
