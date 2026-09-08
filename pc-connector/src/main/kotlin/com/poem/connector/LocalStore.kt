package com.poem.connector

import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDateTime

/**
 * PC 로컬 이력 (SQLite).
 * - 서버와 별개로 처리한 작업/이벤트를 보관 → 오프라인/장애 시 재전송, 감사 추적
 */
class LocalStore(path: String) {
    private val conn: Connection = DriverManager.getConnection("jdbc:sqlite:$path")

    init {
        conn.createStatement().use {
            it.executeUpdate("""
                create table if not exists job_log(
                    job_id integer primary key, type text, plate_no text, external_id text,
                    success integer, error text, processed_at text)""")
            it.executeUpdate("""
                create table if not exists event_log(
                    id integer primary key autoincrement, external_event_id text unique, type text, plate_no text,
                    occurred_at text, sent integer default 0, server_message text)""")
            it.executeUpdate("create table if not exists kv(k text primary key, v text)")
        }
    }

    fun logJob(job: SyncJob, ack: SyncAck) {
        conn.prepareStatement("insert or replace into job_log values(?,?,?,?,?,?,?)").use {
            it.setLong(1, job.jobId); it.setString(2, job.type); it.setString(3, job.plateNo)
            it.setString(4, ack.externalId); it.setInt(5, if (ack.success) 1 else 0)
            it.setString(6, ack.errorMessage); it.setString(7, LocalDateTime.now().toString())
            it.executeUpdate()
        }
    }

    fun saveEvents(events: List<GateEvent>) {
        conn.prepareStatement("insert or ignore into event_log(external_event_id,type,plate_no,occurred_at) values(?,?,?,?)").use { ps ->
            events.forEach { e ->
                ps.setString(1, e.externalEventId ?: "${e.type}-${e.plateNo}-${e.occurredAt}")
                ps.setString(2, e.type); ps.setString(3, e.plateNo); ps.setString(4, e.occurredAt)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    fun unsentEvents(): List<GateEvent> = conn.createStatement().use { st ->
        val rs = st.executeQuery("select external_event_id,type,plate_no,occurred_at from event_log where sent=0 order by id")
        generateSequence { if (rs.next()) GateEvent(rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(1)) else null }.toList()
    }

    fun markSent(events: List<GateEvent>, results: List<GateEventResult>) {
        conn.prepareStatement("update event_log set sent=1, server_message=? where external_event_id=?").use { ps ->
            events.forEachIndexed { i, e ->
                ps.setString(1, results.getOrNull(i)?.message); ps.setString(2, e.externalEventId); ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    fun getLastEventPoll(): LocalDateTime = conn.createStatement().use {
        val rs = it.executeQuery("select v from kv where k='last_event_poll'")
        if (rs.next()) LocalDateTime.parse(rs.getString(1)) else LocalDateTime.now().minusDays(1)
    }

    fun setLastEventPoll(t: LocalDateTime) {
        conn.prepareStatement("insert or replace into kv values('last_event_poll',?)").use { it.setString(1, t.toString()); it.executeUpdate() }
    }
}
