package com.poem.connector

import java.io.File
import java.util.Properties

data class Config(
    val baseUrl: String,
    val apartmentCode: String,
    val apiKey: String,
    val pollIntervalSeconds: Long,
    val heartbeatIntervalSeconds: Long,
    val adapter: String,
    val localDb: String,
) {
    companion object {
        fun load(path: String = "connector.properties"): Config {
            val p = Properties().apply { File(path).inputStream().use { load(it) } }
            return Config(
                baseUrl = p.getProperty("server.baseUrl"),
                apartmentCode = p.getProperty("apartment.code"),
                apiKey = p.getProperty("connector.apiKey"),
                pollIntervalSeconds = p.getProperty("poll.intervalSeconds", "10").toLong(),
                heartbeatIntervalSeconds = p.getProperty("heartbeat.intervalSeconds", "60").toLong(),
                adapter = p.getProperty("adapter", "mock"),
                localDb = p.getProperty("local.db", "./connector.db"),
            )
        }
    }
}
