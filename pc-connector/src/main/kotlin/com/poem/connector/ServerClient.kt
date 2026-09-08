package com.poem.connector

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.net.InetAddress

/** 백엔드 Connector API 클라이언트 */
class ServerClient(private val cfg: Config) {
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        defaultRequest {
            url(cfg.baseUrl)
            header("X-Apartment-Code", cfg.apartmentCode)
            header("X-Connector-Key", cfg.apiKey)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun pollJobs(limit: Int = 20): List<SyncJob> =
        http.get("/api/v1/connector/jobs") { parameter("limit", limit) }.body()

    suspend fun ack(jobId: Long, ack: SyncAck) {
        http.post("/api/v1/connector/jobs/$jobId/ack") { setBody(ack) }
    }

    suspend fun sendEvents(events: List<GateEvent>): List<GateEventResult> =
        http.post("/api/v1/connector/events/batch") { setBody(events) }.body()

    suspend fun heartbeat() {
        http.post("/api/v1/connector/heartbeat") {
            setBody(Heartbeat(version = "0.1.0", hostName = InetAddress.getLocalHost().hostName))
        }
    }

    fun close() = http.close()
}
