package com.poem.connector

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * 연동 프로그램 메인 루프
 *  1) 서버 작업 폴링 → 주차관리 프로그램 반영 → ack
 *  2) 주차관리 프로그램 입/출차 이벤트 수집 → 로컬 저장 → 서버 전송
 *  3) 하트비트
 */
fun main(args: Array<String>) = runBlocking {
    val log = LoggerFactory.getLogger("Connector")
    val cfg = Config.load(args.getOrNull(0) ?: "connector.properties")
    val server = ServerClient(cfg)
    val store = LocalStore(cfg.localDb)
    val adapter: ParkingAdapter = when (cfg.adapter) {
        "mock" -> MockParkingAdapter()
        else -> error("unknown adapter: ${cfg.adapter}")   // TODO: 실제 주차관리 프로그램 어댑터
    }
    log.info("connector started apt={} server={} adapter={}", cfg.apartmentCode, cfg.baseUrl, cfg.adapter)

    // 1) 작업 처리 루프
    launch {
        while (isActive) {
            try {
                val jobs = server.pollJobs()
                for (job in jobs) {
                    val ack = try {
                        when (job.type) {
                            "REGISTER", "UPDATE" -> SyncAck(true, externalId = adapter.registerVisitor(job))
                            "CANCEL" -> { adapter.cancelVisitor(job); SyncAck(true) }
                            else -> SyncAck(false, errorMessage = "unknown job type ${job.type}")
                        }
                    } catch (e: Exception) {
                        log.error("job {} failed", job.jobId, e)
                        SyncAck(false, errorMessage = e.message ?: e.javaClass.simpleName)
                    }
                    store.logJob(job, ack)
                    server.ack(job.jobId, ack)
                }
            } catch (e: Exception) {
                log.warn("poll failed: {}", e.message)
            }
            delay(cfg.pollIntervalSeconds * 1000)
        }
    }

    // 2) 이벤트 수집/전송 루프 (로컬 저장 후 전송 → 서버 장애 시에도 유실 없음)
    launch {
        while (isActive) {
            try {
                val since = store.getLastEventPoll()
                val now = LocalDateTime.now()
                val events = adapter.fetchGateEvents(since)
                if (events.isNotEmpty()) store.saveEvents(events)
                store.setLastEventPoll(now)

                val pending = store.unsentEvents()
                if (pending.isNotEmpty()) {
                    val results = server.sendEvents(pending)
                    store.markSent(pending, results)
                    log.info("sent {} gate events", pending.size)
                }
            } catch (e: Exception) {
                log.warn("event sync failed: {}", e.message)
            }
            delay(cfg.pollIntervalSeconds * 1000)
        }
    }

    // 3) 하트비트
    launch {
        while (isActive) {
            runCatching { server.heartbeat() }.onFailure { log.warn("heartbeat failed: {}", it.message) }
            delay(cfg.heartbeatIntervalSeconds * 1000)
        }
    }
}
