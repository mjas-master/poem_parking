package com.poem.parking.service

import com.poem.parking.domain.Apartment
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.ceil

@Component
class FeeCalculator {
    /** 체류 분 계산 (올림) */
    fun minutesBetween(from: LocalDateTime, to: LocalDateTime): Int {
        val sec = Duration.between(from, to).seconds.coerceAtLeast(0)
        return ceil(sec / 60.0).toInt()
    }

    /**
     * 요금 산정: 무료시간 초과분을 과금단위로 올림하여 단위요금 부과, 일 상한 적용.
     * 예) 무료 30분, 10분당 500원, 일 상한 10,000원 / 체류 95분 -> 초과 65분 -> 7단위 -> 3,500원
     */
    fun calculate(apt: Apartment, durationMinutes: Int): Int {
        val billable = (durationMinutes - apt.freeMinutes).coerceAtLeast(0)
        if (billable == 0) return 0
        val units = ceil(billable / apt.unitMinutes.toDouble()).toInt()
        var fee = units * apt.unitFee
        if (apt.dailyCapFee > 0) {
            val days = ceil(durationMinutes / (24.0 * 60)).toInt().coerceAtLeast(1)
            fee = fee.coerceAtMost(apt.dailyCapFee * days)
        }
        return fee
    }
}
