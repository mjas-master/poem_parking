package com.poem.parking.controller

import com.poem.parking.config.CurrentUser
import com.poem.parking.dto.*
import com.poem.parking.service.ConnectorService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * PC 연동 프로그램(에이전트) 전용 API.
 * 인증: 헤더 X-Apartment-Code, X-Connector-Key
 */
@RestController
@RequestMapping("/api/v1/connector")
class ConnectorController(
    private val connectorService: ConnectorService,
    private val currentUser: CurrentUser,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 처리할 작업 폴링 (long-polling 아님, 주기 호출) */
    @GetMapping("/jobs")
    fun pollJobs(@RequestParam(defaultValue = "20") limit: Int): List<SyncJobResponse> =
        connectorService.pollJobs(currentUser.connectorApartment(), limit.coerceIn(1, 100))

    /** 작업 처리 결과 보고 */
    @PostMapping("/jobs/{jobId}/ack")
    fun ack(@PathVariable jobId: Long, @RequestBody req: SyncAckRequest): ResponseEntity<Void> {
        connectorService.ack(currentUser.connectorApartment(), jobId, req)
        return ResponseEntity.ok().build()
    }

    /** 입/출차 이벤트 업로드 (단건) */
    @PostMapping("/events")
    fun event(@Valid @RequestBody ev: GateEventRequest): GateEventResult =
        connectorService.handleGateEvent(currentUser.connectorApartment(), ev)

    /** 입/출차 이벤트 업로드 (배치, 오프라인 복구용) */
    @PostMapping("/events/batch")
    fun events(@Valid @RequestBody evs: List<GateEventRequest>): List<GateEventResult> {
        val apt = currentUser.connectorApartment()
        return evs.map { connectorService.handleGateEvent(apt, it) }
    }

    @PostMapping("/heartbeat")
    fun heartbeat(@RequestBody(required = false) hb: ConnectorHeartbeat?): Map<String, Any> {
        val apt = currentUser.connectorApartment()
        log.debug("heartbeat from {} {}", apt.code, hb)
        return mapOf("serverTime" to LocalDateTime.now(), "apartment" to apt.code)
    }
}
