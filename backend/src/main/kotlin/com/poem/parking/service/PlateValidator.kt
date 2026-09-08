package com.poem.parking.service

import com.poem.parking.exception.BadRequestException
import org.springframework.stereotype.Component

/**
 * 한국 차량번호 검증/정규화.
 *  - 신형: 12가3456, 123가4567
 *  - 구형(지역): 서울12가3456
 *  - 영업용/전기차 등 한글 범위는 포괄적으로 허용
 */
@Component
class PlateValidator {
    private val newFormat = Regex("^\\d{2,3}[가-힣]\\d{4}$")
    private val regionFormat = Regex("^[가-힣]{2}\\d{1,2}[가-힣]\\d{4}$")

    fun normalize(raw: String): String = raw.replace(Regex("[\\s\\-]"), "").trim()

    fun validateAndNormalize(raw: String): String {
        val plate = normalize(raw)
        if (plate.isBlank()) throw BadRequestException("PLATE_REQUIRED", "차량번호를 입력하세요.")
        if (!(newFormat.matches(plate) || regionFormat.matches(plate))) {
            throw BadRequestException("PLATE_INVALID", "차량번호 형식이 올바르지 않습니다. (예: 12가3456)")
        }
        return plate
    }
}
