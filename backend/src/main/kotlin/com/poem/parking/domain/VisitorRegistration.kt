package com.poem.parking.domain

import jakarta.persistence.*
import java.time.LocalDateTime

enum class RegistrationStatus {
    PENDING,    // 앱에서 등록됨, PC 연동 대기
    SYNCED,     // PC 주차관리 프로그램에 반영 완료
    ENTERED,    // 차량 입차 확인
    EXITED,     // 차량 출차 완료
    CANCELLED,  // 사용자 취소
    EXPIRED,    // 방문 예정 기간 경과
    FAILED,     // PC 반영 실패
}

/** 방문차량 등록 요청 */
@Entity
@Table(name = "visitor_registrations", indexes = [
    Index(name = "idx_reg_household", columnList = "household_id"),
    Index(name = "idx_reg_plate", columnList = "plateNo"),
])
class VisitorRegistration(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id")
    var household: Household,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    var registeredBy: User,
    /** 정규화된 차량번호 (공백 제거) 예: 12가3456 */
    @Column(nullable = false, length = 20)
    var plateNo: String,
    var visitorName: String? = null,
    var visitorPhone: String? = null,
    var purpose: String? = null,
    @Column(nullable = false)
    var visitFrom: LocalDateTime,
    @Column(nullable = false)
    var visitTo: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: RegistrationStatus = RegistrationStatus.PENDING,
    /** PC 측 식별자 (연동 후 채워짐) */
    var externalId: String? = null,
    var syncedAt: LocalDateTime? = null,
    var failReason: String? = null,
) : BaseEntity()
