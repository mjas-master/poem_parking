package com.poem.parking.dto

import com.poem.parking.domain.RegistrationStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class RegisterVisitorRequest(
    @field:NotBlank(message = "차량번호는 필수입니다.")
    @field:Size(max = 20)
    val plateNo: String,
    @field:Size(max = 30) val visitorName: String? = null,
    @field:Size(max = 20) val visitorPhone: String? = null,
    @field:Size(max = 100) val purpose: String? = null,
    @field:NotNull val visitFrom: LocalDateTime,
    @field:NotNull val visitTo: LocalDateTime,
)

data class RegistrationResponse(
    val id: Long,
    val plateNo: String,
    val visitorName: String?,
    val visitorPhone: String?,
    val purpose: String?,
    val visitFrom: LocalDateTime,
    val visitTo: LocalDateTime,
    val status: RegistrationStatus,
    val syncedAt: LocalDateTime?,
    val failReason: String?,
    val createdAt: LocalDateTime,
)

data class HistoryResponse(
    val id: Long,
    val registrationId: Long?,
    val plateNo: String,
    val visitorName: String?,
    val enteredAt: LocalDateTime,
    val exitedAt: LocalDateTime?,
    val durationMinutes: Int?,
    val fee: Int?,
    val inProgress: Boolean,
)

data class FeeSummaryResponse(
    val year: Int,
    val month: Int,
    val visitCount: Int,
    val totalMinutes: Int,
    val totalFee: Int,
    val byPlate: List<PlateFee>,
)

data class PlateFee(val plateNo: String, val visitCount: Int, val totalMinutes: Int, val totalFee: Int)

data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
