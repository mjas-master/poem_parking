package com.poem.connector

import kotlinx.serialization.Serializable

/** 서버 API DTO — backend/dto/ConnectorDtos.kt 와 동일 구조 */
@Serializable
data class SyncJob(
    val jobId: Long, val type: String, val registrationId: Long, val plateNo: String,
    val dong: String, val ho: String, val visitorName: String? = null,
    val visitFrom: String, val visitTo: String, val attempts: Int,
)

@Serializable
data class SyncAck(val success: Boolean, val externalId: String? = null, val errorMessage: String? = null)

@Serializable
data class GateEvent(val type: String, val plateNo: String, val occurredAt: String, val externalEventId: String? = null)

@Serializable
data class GateEventResult(val accepted: Boolean, val matched: Boolean, val historyId: Long? = null, val message: String)

@Serializable
data class Heartbeat(val version: String, val hostName: String)
