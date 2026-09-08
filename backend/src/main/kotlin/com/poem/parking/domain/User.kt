package com.poem.parking.domain

import jakarta.persistence.*

enum class UserRole { RESIDENT, ADMIN }

@Entity
@Table(name = "users")
class User(
    @Column(nullable = false, unique = true, length = 20)
    var phone: String,
    @Column(nullable = false)
    var name: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "household_id")
    var household: Household? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.RESIDENT,
    /** 아파트(세대) 인증 완료 여부 */
    var verified: Boolean = false,
    /** 푸시 토큰 (FCM) - 추후 사용 */
    var pushToken: String? = null,
) : BaseEntity()
