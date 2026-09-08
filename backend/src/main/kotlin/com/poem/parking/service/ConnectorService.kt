package com.poem.parking.service

import com.poem.parking.domain.*
import com.poem.parking.dto.*
import com.poem.parking.exception.NotFoundException
import com.poem.parking.repository.SyncOutboxRepository
import com.poem.parking.repository.VisitHistoryRepository
import com.poem.parking.repository.VisitorRegistrationRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** PC 연동 프로그램(에이전트)과의 통신을 담당 */
@Service
@Transactional
class ConnectorService(
    private val outboxRepository: SyncOutboxRepository,
    private val registrationRepository: VisitorRegistrationRepository,
    private val historyRepository: VisitHistoryRepository,
    private val plateValidator: PlateValidator,
    private val feeCalculator: FeeCalculator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val MAX_ATTEMPTS = 5
        const val REDELIVER_AFTER_MINUTES = 5L
    }

    /** PENDING 작업을 꺼내어 DELIVERED로 표시하고 반환 */
    fun pollJobs(apt: Apartment, limit: Int): List<SyncJobResponse> {
        val jobs = outboxRepository.findByApartmentAndStatusOrderByCreatedAtAsc(apt, SyncJobStatus.PENDING, PageRequest.of(0, limit))
        val now = LocalDateTime.now()
        return jobs.map { job ->
            job.status = SyncJobStatus.DELIVERED
            job.deliveredAt = now
            job.attempts += 1
            val r = job.registration
            SyncJobResponse(
                jobId = job.id!!, type = job.type, registrationId = r.id!!, plateNo = r.plateNo,
                dong = r.household.dong, ho = r.household.ho, visitorName = r.visitorName,
                visitFrom = r.visitFrom, visitTo = r.visitTo, attempts = job.attempts,
            )
        }
    }

    fun ack(apt: Apartment, jobId: Long, req: SyncAckRequest) {
        val job = outboxRepository.findByIdAndApartment(jobId, apt) ?: throw NotFoundException("작업을 찾을 수 없습니다.")
        val reg = job.registration
        if (req.success) {
            job.status = SyncJobStatus.ACKED
            job.ackedAt = LocalDateTime.now()
            when (job.type) {
                SyncJobType.REGISTER, SyncJobType.UPDATE -> {
                    if (reg.status == RegistrationStatus.PENDING) reg.status = RegistrationStatus.SYNCED
                    reg.syncedAt = LocalDateTime.now()
                    reg.externalId = req.externalId ?: reg.externalId
                    reg.failReason = null
                }
                SyncJobType.CANCEL -> { /* 이미 CANCELLED */ }
            }
        } else {
            job.lastError = req.errorMessage
            if (job.attempts >= MAX_ATTEMPTS) {
                job.status = SyncJobStatus.FAILED
                if (job.type == SyncJobType.REGISTER) {
                    reg.status = RegistrationStatus.FAILED
                    reg.failReason = req.errorMessage ?: "주차관리 프로그램 반영 실패"
                }
            } else {
                job.status = SyncJobStatus.PENDING // 재시도
            }
        }
    }

    /** 입/출차 이벤트 처리 */
    fun handleGateEvent(apt: Apartment, ev: GateEventRequest): GateEventResult {
        val plate = plateValidator.normalize(ev.plateNo)
        return when (ev.type) {
            GateEventType.ENTRY -> onEntry(apt, plate, ev.occurredAt)
            GateEventType.EXIT -> onExit(apt, plate, ev.occurredAt)
        }
    }

    private fun onEntry(apt: Apartment, plate: String, at: LocalDateTime): GateEventResult {
        // 등록되지 않은 차량 이벤트는 기록하지 않음 (입주민 차량 등은 PC 프로그램이 별도 관리)
        val reg = registrationRepository.findActiveForPlateAt(
            apt, plate, at, setOf(RegistrationStatus.PENDING, RegistrationStatus.SYNCED)
        ).firstOrNull() ?: return GateEventResult(true, false, null, "등록된 방문차량이 아닙니다.")

        // 중복 입차 이벤트 방지
        historyRepository.findFirstByRegistrationAndExitedAtIsNull(reg)?.let {
            return GateEventResult(true, true, it.id, "이미 입차 처리된 차량입니다.")
        }
        reg.status = RegistrationStatus.ENTERED
        val h = historyRepository.save(VisitHistory(household = reg.household, registration = reg, plateNo = plate, enteredAt = at))
        return GateEventResult(true, true, h.id, "입차 기록 완료")
    }

    private fun onExit(apt: Apartment, plate: String, at: LocalDateTime): GateEventResult {
        val h = historyRepository.findOpenByPlate(apt, plate).firstOrNull()
            ?: return GateEventResult(true, false, null, "입차 기록이 없는 차량입니다.")
        h.exitedAt = at
        h.durationMinutes = feeCalculator.minutesBetween(h.enteredAt, at)
        h.fee = feeCalculator.calculate(apt, h.durationMinutes!!)
        h.registration?.let { it.status = RegistrationStatus.EXITED }
        return GateEventResult(true, true, h.id, "출차 기록 완료 (${h.durationMinutes}분, ${h.fee}원)")
    }

    /** DELIVERED 후 ack가 오지 않은 작업은 재전달 대상으로 되돌림 */
    @Scheduled(fixedDelay = 60_000)
    fun redeliverStale() {
        val stale = outboxRepository.findByStatusAndDeliveredAtBefore(
            SyncJobStatus.DELIVERED, LocalDateTime.now().minusMinutes(REDELIVER_AFTER_MINUTES)
        )
        stale.forEach {
            if (it.attempts >= MAX_ATTEMPTS) {
                it.status = SyncJobStatus.FAILED
                it.lastError = "ack timeout"
                if (it.type == SyncJobType.REGISTER) {
                    it.registration.status = RegistrationStatus.FAILED
                    it.registration.failReason = "주차관리 프로그램 응답 없음"
                }
            } else {
                it.status = SyncJobStatus.PENDING
            }
        }
        if (stale.isNotEmpty()) log.info("redelivered {} stale sync jobs", stale.size)
    }
}
