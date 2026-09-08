package com.poem.parking.domain

import jakarta.persistence.*
import java.time.LocalDateTime

enum class SyncJobType { REGISTER, CANCEL, UPDATE }
enum class SyncJobStatus { PENDING, DELIVERED, ACKED, FAILED }

/**
 * PC 연동 프로그램으로 전달할 작업 큐 (Outbox 패턴).
 * PC 프로그램이 폴링으로 가져가고(DELIVERED) 처리 후 ack(ACKED)한다.
 */
@Entity
@Table(name = "sync_outbox", indexes = [Index(name = "idx_outbox_status", columnList = "apartment_id,status")])
class SyncOutbox(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id")
    var apartment: Apartment,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id")
    var registration: VisitorRegistration,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: SyncJobType,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SyncJobStatus = SyncJobStatus.PENDING,
    var attempts: Int = 0,
    var deliveredAt: LocalDateTime? = null,
    var ackedAt: LocalDateTime? = null,
    var lastError: String? = null,
) : BaseEntity()
