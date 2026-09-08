package com.poem.connector

import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * 주차관리 프로그램(3rd-party)과의 연동 추상화.
 * 실제 프로그램이 정해지면 구현체를 추가한다. (DB 직접 접근 / 파일 감시 / 벤더 API / UI 자동화 등)
 */
interface ParkingAdapter {
    /** 방문차량을 주차관리 프로그램에 등록. 반환값: 프로그램 내부 ID */
    fun registerVisitor(job: SyncJob): String
    /** 등록 취소 */
    fun cancelVisitor(job: SyncJob)
    /** 마지막 조회 이후 발생한 입/출차 이벤트를 가져온다 */
    fun fetchGateEvents(since: LocalDateTime): List<GateEvent>
}

/** 개발용 목업: 콘솔 로그만 남기고, 등록 후 일정 시간 뒤 가짜 입/출차 이벤트를 만든다 */
class MockParkingAdapter : ParkingAdapter {
    private val log = LoggerFactory.getLogger(javaClass)
    private val registered = linkedMapOf<String, LocalDateTime>()  // plate -> registeredAt
    private val emitted = mutableSetOf<String>()

    override fun registerVisitor(job: SyncJob): String {
        log.info("[MOCK PARKING] REGISTER plate={} {}동 {}호 {}~{}", job.plateNo, job.dong, job.ho, job.visitFrom, job.visitTo)
        registered[job.plateNo] = LocalDateTime.now()
        return "MOCK-${job.registrationId}"
    }

    override fun cancelVisitor(job: SyncJob) {
        log.info("[MOCK PARKING] CANCEL plate={}", job.plateNo)
        registered.remove(job.plateNo)
    }

    override fun fetchGateEvents(since: LocalDateTime): List<GateEvent> {
        val now = LocalDateTime.now()
        val out = mutableListOf<GateEvent>()
        for ((plate, at) in registered) {
            if (now.isAfter(at.plusSeconds(30)) && "$plate:ENTRY" !in emitted) {
                emitted += "$plate:ENTRY"
                out += GateEvent("ENTRY", plate, now.withNano(0).toString(), "mock-$plate-in")
            }
            if (now.isAfter(at.plusSeconds(120)) && "$plate:EXIT" !in emitted) {
                emitted += "$plate:EXIT"
                out += GateEvent("EXIT", plate, now.withNano(0).toString(), "mock-$plate-out")
            }
        }
        return out
    }
}
