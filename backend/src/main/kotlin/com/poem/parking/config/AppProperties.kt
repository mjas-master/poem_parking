package com.poem.parking.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val jwt: Jwt,
    val connector: Connector,
    val mock: Mock,
) {
    data class Jwt(val secret: String, val expirationMinutes: Long)
    /** apartmentCode -> apiKey */
    data class Connector(val apiKeys: Map<String, String> = emptyMap())
    data class Mock(val apartmentVerifyCode: String, val smsOtp: String)
}
