package com.poem.parking.config

import com.poem.parking.domain.Apartment
import com.poem.parking.domain.User
import com.poem.parking.exception.ForbiddenException
import com.poem.parking.exception.UnauthorizedException
import com.poem.parking.repository.ApartmentRepository
import com.poem.parking.repository.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/** 컨트롤러에서 현재 인증 주체를 편하게 꺼내기 위한 헬퍼 */
@Component
class CurrentUser(
    private val userRepository: UserRepository,
    private val apartmentRepository: ApartmentRepository,
) {
    fun user(): User {
        val p = SecurityContextHolder.getContext().authentication?.principal as? Principal.AppUser
            ?: throw UnauthorizedException("로그인이 필요합니다.")
        return userRepository.findWithHouseholdById(p.userId) ?: throw UnauthorizedException("사용자를 찾을 수 없습니다.")
    }

    /** 아파트 인증까지 완료한 사용자 */
    fun verifiedUser(): User {
        val u = user()
        if (!u.verified || u.household == null) throw ForbiddenException("아파트 인증이 필요합니다.")
        return u
    }

    fun connectorApartment(): Apartment {
        val p = SecurityContextHolder.getContext().authentication?.principal as? Principal.Connector
            ?: throw UnauthorizedException("연동 인증이 필요합니다.")
        return apartmentRepository.findByCode(p.apartmentCode) ?: throw UnauthorizedException("아파트 코드가 없습니다.")
    }
}
