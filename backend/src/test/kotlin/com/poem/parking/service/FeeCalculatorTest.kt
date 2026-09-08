package com.poem.parking.service

import com.poem.parking.domain.Apartment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FeeCalculatorTest {
    private val calc = FeeCalculator()
    private val apt = Apartment(code = "T", name = "T", freeMinutes = 30, unitMinutes = 10, unitFee = 500, dailyCapFee = 10000)

    @Test
    fun `무료시간 이내는 0원`() = assertEquals(0, calc.calculate(apt, 30))

    @Test
    fun `초과분은 단위 올림 과금`() {
        assertEquals(500, calc.calculate(apt, 31))    // 1분 초과 -> 1단위
        assertEquals(3500, calc.calculate(apt, 95))   // 65분 초과 -> 7단위
    }

    @Test
    fun `일 상한 적용`() = assertEquals(10000, calc.calculate(apt, 12 * 60))

    @Test
    fun `이틀 주차는 상한 2배`() = assertEquals(20000, calc.calculate(apt, 47 * 60))

    @Test
    fun `분 계산은 올림`() {
        val from = LocalDateTime.of(2026, 9, 8, 10, 0, 0)
        assertEquals(31, calc.minutesBetween(from, from.plusMinutes(30).plusSeconds(1)))
    }
}
