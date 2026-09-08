package com.poem.parking.dto

import com.poem.parking.domain.SyncJobType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

/** PC 연동 프로그램이 폴링으로 가져가는 작업 */
data class SyncJobResponse(
    val jobId: Long,
    val type: SyncJobType,
    val registrationId: Long,
    val plateNo: String,
    val dong: String,
    val ho: String,
    val visitorName: String?,
    val visitFrom: LocalDateTime,
    val visitTo: LocalDateTime,
    val attempts: Int,
)

data class SyncAckRequest(
    val success: Boolean,
    /** 주차관리 프로그램 내부 ID */
    val externalId: String? = null,
    val errorMessage: String? = null,
)

enum class GateEventType { ENTRY, EXIT }

/** 차량 입/출차 이벤트 */
data class GateEventRequest(
    @field:NotNull val type: GateEventType,
    @field:NotBlank val plateNo: String,
    @field:NotNull val occurredAt: LocalDateTime,
    /** PC 측 이벤트 ID (중복 방지용) */
    val externalEventId: String? = null,
)

data class GateEventResult(
    val accepted: Boolean,
    val matched: Boolean,
    val historyId: Long?,
    val message: String,
)

data class ConnectorHeartbeat(
    val version: String? = null,
    val hostName: String? = null,
)
