package com.poem.parking.service

import com.poem.parking.domain.User
import com.poem.parking.domain.VisitHistory
import com.poem.parking.dto.FeeSummaryResponse
import com.poem.parking.dto.HistoryResponse
import com.poem.parking.dto.PageResponse
import com.poem.parking.dto.PlateFee
import com.poem.parking.exception.BadRequestException
import com.poem.parking.exception.NotFoundException
import com.poem.parking.repository.VisitHistoryRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class HistoryService(
    private val historyRepository: VisitHistoryRepository,
    private val feeCalculator: FeeCalculator,
) {
    fun list(user: User, page: Int, size: Int): PageResponse<HistoryResponse> {
        val household = user.household ?: throw BadRequestException("NO_HOUSEHOLD", "세대 정보가 없습니다.")
        val p = historyRepository.findByHouseholdAndDeletedByUserFalseOrderByEnteredAtDesc(household, PageRequest.of(page, size))
        return PageResponse(p.content.map { toResponse(it) }, p.number, p.size, p.totalElements, p.totalPages)
    }

    /** 사용자 삭제 = 소프트 삭제. 요금 집계 근거는 보존 */
    fun delete(user: User, id: Long) {
        val household = user.household ?: throw BadRequestException("NO_HOUSEHOLD", "세대 정보가 없습니다.")
        val h = historyRepository.findByIdAndHousehold(id, household) ?: throw NotFoundException("이력을 찾을 수 없습니다.")
        if (h.exitedAt == null) throw BadRequestException("IN_PROGRESS", "주차 중인 차량 이력은 삭제할 수 없습니다.")
        h.deletedByUser = true
        h.deletedAt = LocalDateTime.now()
    }

    /** 월별 요금 집계 (소프트 삭제된 이력도 포함) */
    fun monthlySummary(user: User, year: Int, month: Int): FeeSummaryResponse {
        val household = user.household ?: throw BadRequestException("NO_HOUSEHOLD", "세대 정보가 없습니다.")
        val from = LocalDate.of(year, month, 1).atStartOfDay()
        val to = from.plusMonths(1)
        val rows = historyRepository.findByHouseholdBetween(household, from, to)
        val apt = household.apartment

        // 진행 중인 건은 현재 시각 기준 잠정 요금
        fun minutesOf(h: VisitHistory) = h.durationMinutes ?: feeCalculator.minutesBetween(h.enteredAt, LocalDateTime.now())
        fun feeOf(h: VisitHistory) = h.fee ?: feeCalculator.calculate(apt, minutesOf(h))

        val byPlate = rows.groupBy { it.plateNo }.map { (plate, list) ->
            PlateFee(plate, list.size, list.sumOf { minutesOf(it) }, list.sumOf { feeOf(it) })
        }.sortedByDescending { it.totalFee }

        return FeeSummaryResponse(
            year = year, month = month, visitCount = rows.size,
            totalMinutes = rows.sumOf { minutesOf(it) }, totalFee = rows.sumOf { feeOf(it) }, byPlate = byPlate,
        )
    }

    fun toResponse(h: VisitHistory) = HistoryResponse(
        id = h.id!!, registrationId = h.registration?.id, plateNo = h.plateNo,
        visitorName = h.registration?.visitorName, enteredAt = h.enteredAt, exitedAt = h.exitedAt,
        durationMinutes = h.durationMinutes, fee = h.fee, inProgress = h.exitedAt == null,
    )
}
