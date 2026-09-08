package com.poem.parking.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/** 실제 입/출차 이력 (PC 연동 프로그램이 이벤트로 올려줌) */
@Entity
@Table(name = "visit_histories", indexes = [
    Index(name = "idx_hist_household", columnList = "household_id"),
    Index(name = "idx_hist_entered", columnList = "enteredAt"),
])
class VisitHistory(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id")
    var household: Household,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_id")
    var registration: VisitorRegistration? = null,
    @Column(nullable = false, length = 20)
    var plateNo: String,
    @Column(nullable = false)
    var enteredAt: LocalDateTime,
    var exitedAt: LocalDateTime? = null,
    /** 체류 시간(분) - 출차 시 계산 */
    var durationMinutes: Int? = null,
    /** 산정 주차료(원) - 출차 시 계산 */
    var fee: Int? = null,
    /** 사용자 소프트 삭제 (집계에는 포함, 목록에서만 숨김) */
    var deletedByUser: Boolean = false,
    var deletedAt: LocalDateTime? = null,
) : BaseEntity()
