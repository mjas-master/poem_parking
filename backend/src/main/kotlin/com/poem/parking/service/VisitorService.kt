package com.poem.parking.service

import com.poem.parking.domain.*
import com.poem.parking.dto.PageResponse
import com.poem.parking.dto.RegisterVisitorRequest
import com.poem.parking.dto.RegistrationResponse
import com.poem.parking.exception.BadRequestException
import com.poem.parking.exception.ConflictException
import com.poem.parking.exception.NotFoundException
import com.poem.parking.repository.SyncOutboxRepository
import com.poem.parking.repository.VisitorRegistrationRepository
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class VisitorService(
    private val registrationRepository: VisitorRegistrationRepository,
    private val outboxRepository: SyncOutboxRepository,
    private val plateValidator: PlateValidator,
) {
    companion object {
        val ACTIVE_STATUSES = setOf(RegistrationStatus.PENDING, RegistrationStatus.SYNCED, RegistrationStatus.ENTERED)
        const val MAX_ACTIVE_PER_HOUSEHOLD = 5
        const val MAX_VISIT_HOURS = 24 * 7L
    }

    /** 방문차량 등록: 검증 → 저장 → Outbox 큐 적재 */
    fun register(user: User, req: RegisterVisitorRequest): RegistrationResponse {
        val household = user.household ?: throw BadRequestException("NO_HOUSEHOLD", "세대 정보가 없습니다.")
        val plate = plateValidator.validateAndNormalize(req.plateNo)

        if (!req.visitTo.isAfter(req.visitFrom)) throw BadRequestException("PERIOD_INVALID", "방문 종료 시간은 시작 시간 이후여야 합니다.")
        if (req.visitTo.isBefore(LocalDateTime.now())) throw BadRequestException("PERIOD_PAST", "이미 지난 시간입니다.")
        if (java.time.Duration.between(req.visitFrom, req.visitTo).toHours() > MAX_VISIT_HOURS) {
            throw BadRequestException("PERIOD_TOO_LONG", "방문 기간은 최대 7일까지 등록할 수 있습니다.")
        }

        val active = registrationRepository.findByHouseholdAndStatusInOrderByVisitFromAsc(household, ACTIVE_STATUSES)
        if (active.size >= MAX_ACTIVE_PER_HOUSEHOLD) {
            throw ConflictException("LIMIT_EXCEEDED", "세대당 동시 등록 가능 차량은 ${MAX_ACTIVE_PER_HOUSEHOLD}대입니다.")
        }
        val overlapping = registrationRepository.findOverlapping(household.apartment, plate, req.visitFrom, req.visitTo, ACTIVE_STATUSES)
        if (overlapping.isNotEmpty()) {
            throw ConflictException("DUPLICATE_PLATE", "해당 기간에 이미 등록된 차량번호입니다.")
        }

        val reg = registrationRepository.save(
            VisitorRegistration(
                household = household, registeredBy = user, plateNo = plate,
                visitorName = req.visitorName?.trim(), visitorPhone = req.visitorPhone?.trim(),
                purpose = req.purpose?.trim(), visitFrom = req.visitFrom, visitTo = req.visitTo,
            )
        )
        outboxRepository.save(SyncOutbox(apartment = household.apartment, registration = reg, type = SyncJobType.REGISTER))
        return toResponse(reg)
    }

    fun list(user: User, page: Int, size: Int): PageResponse<RegistrationResponse> {
        val household = user.household ?: throw BadRequestException("NO_HOUSEHOLD", "세대 정보가 없습니다.")
        val p = registrationRepository.findByHouseholdOrderByVisitFromDesc(household, PageRequest.of(page, size))
        return PageResponse(p.content.map { toResponse(it) }, p.number, p.size, p.totalElements, p.totalPages)
    }

    fun active(user: User): List<RegistrationResponse> {
        val household = user.household ?: throw BadRequestException("NO_HOUSEHOLD", "세대 정보가 없습니다.")
        return registrationRepository.findByHouseholdAndStatusInOrderByVisitFromAsc(household, ACTIVE_STATUSES).map { toResponse(it) }
    }

    fun get(user: User, id: Long): RegistrationResponse = toResponse(find(user, id))

    /** 취소: PC에 이미 반영되었으면 CANCEL 작업을 큐에 넣음 */
    fun cancel(user: User, id: Long): RegistrationResponse {
        val reg = find(user, id)
        if (reg.status !in ACTIVE_STATUSES) throw ConflictException("NOT_CANCELLABLE", "취소할 수 없는 상태입니다: ${reg.status}")
        if (reg.status == RegistrationStatus.ENTERED) throw ConflictException("ALREADY_ENTERED", "이미 입차한 차량은 취소할 수 없습니다.")
        val needSync = reg.status == RegistrationStatus.SYNCED
        reg.status = RegistrationStatus.CANCELLED
        if (needSync) {
            outboxRepository.save(SyncOutbox(apartment = reg.household.apartment, registration = reg, type = SyncJobType.CANCEL))
        }
        return toResponse(reg)
    }

    /** 방문 예정 기간이 지난 등록은 만료 처리 (10분마다) */
    @Scheduled(fixedDelay = 600_000)
    fun expireOld() {
        val targets = registrationRepository.findByStatusInAndVisitToBefore(
            setOf(RegistrationStatus.PENDING, RegistrationStatus.SYNCED), LocalDateTime.now()
        )
        targets.forEach { it.status = RegistrationStatus.EXPIRED }
    }

    private fun find(user: User, id: Long): VisitorRegistration {
        val household = user.household ?: throw BadRequestException("NO_HOUSEHOLD", "세대 정보가 없습니다.")
        return registrationRepository.findByIdAndHousehold(id, household) ?: throw NotFoundException("등록 정보를 찾을 수 없습니다.")
    }

    fun toResponse(r: VisitorRegistration) = RegistrationResponse(
        id = r.id!!, plateNo = r.plateNo, visitorName = r.visitorName, visitorPhone = r.visitorPhone,
        purpose = r.purpose, visitFrom = r.visitFrom, visitTo = r.visitTo, status = r.status,
        syncedAt = r.syncedAt, failReason = r.failReason, createdAt = r.createdAt,
    )
}
