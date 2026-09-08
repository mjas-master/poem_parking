package com.poem.parking.domain

import jakarta.persistence.*

/** 아파트 단지. 연동 프로그램(PC) 1대가 단지 1개를 담당한다고 가정 */
@Entity
@Table(name = "apartments")
class Apartment(
    @Column(nullable = false, unique = true, length = 20)
    var code: String,                 // 예: APT-0001
    @Column(nullable = false)
    var name: String,
    var address: String? = null,
    /** 무료 주차 시간(분) */
    var freeMinutes: Int = 30,
    /** 과금 단위(분) */
    var unitMinutes: Int = 10,
    /** 단위 요금(원) */
    var unitFee: Int = 500,
    /** 1일 상한 요금(원), 0이면 무제한 */
    var dailyCapFee: Int = 10000,
) : BaseEntity()
