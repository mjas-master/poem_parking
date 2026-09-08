package com.poem.parking.domain

import jakarta.persistence.*

/** 세대 (동/호) */
@Entity
@Table(name = "households", uniqueConstraints = [UniqueConstraint(columnNames = ["apartment_id", "dong", "ho"])])
class Household(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id")
    var apartment: Apartment,
    @Column(nullable = false, length = 10)
    var dong: String,
    @Column(nullable = false, length = 10)
    var ho: String,
) : BaseEntity()
