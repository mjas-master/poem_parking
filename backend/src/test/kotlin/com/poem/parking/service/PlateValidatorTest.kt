package com.poem.parking.service

import com.poem.parking.exception.BadRequestException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PlateValidatorTest {
    private val v = PlateValidator()

    @Test
    fun `공백과 하이픈을 제거하고 정규화한다`() {
        assertEquals("12가3456", v.validateAndNormalize(" 12가 3456 "))
        assertEquals("123가4567", v.validateAndNormalize("123가-4567"))
        assertEquals("서울12가3456", v.validateAndNormalize("서울 12가 3456"))
    }

    @Test
    fun `잘못된 형식은 예외`() {
        assertThrows(BadRequestException::class.java) { v.validateAndNormalize("ABC1234") }
        assertThrows(BadRequestException::class.java) { v.validateAndNormalize("1가3456") }
        assertThrows(BadRequestException::class.java) { v.validateAndNormalize("") }
    }
}
