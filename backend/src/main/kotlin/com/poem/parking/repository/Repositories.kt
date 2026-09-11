package com.poem.parking.repository

import com.poem.parking.domain.*
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ApartmentRepository : JpaRepository<Apartment, Long> {
    fun findByCode(code: String): Apartment?
}

interface HouseholdRepository : JpaRepository<Household, Long> {
    fun findByApartmentAndDongAndHo(apartment: Apartment, dong: String, ho: String): Household?
}

interface UserRepository : JpaRepository<User, Long> {
    fun findByPhone(phone: String): User?
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = ["household", "household.apartment"])
    fun findWithHouseholdById(id: Long): User?
}

interface VisitorRegistrationRepository : JpaRepository<VisitorRegistration, Long> {
    fun findByHouseholdOrderByVisitFromDesc(household: Household, pageable: Pageable): Page<VisitorRegistration>

    fun findByHouseholdAndStatusInOrderByVisitFromAsc(
        household: Household, statuses: Collection<RegistrationStatus>
    ): List<VisitorRegistration>

    fun findByIdAndHousehold(id: Long, household: Household): VisitorRegistration?

    /** 동일 차량이 겹치는 기간에 이미 활성 등록되어 있는지 */
    @Query("""
        select r from VisitorRegistration r
        where r.household.apartment = :apartment
          and r.plateNo = :plateNo
          and r.status in :statuses
          and r.visitFrom < :to and r.visitTo > :from
    """)
    fun findOverlapping(
        @Param("apartment") apartment: Apartment,
        @Param("plateNo") plateNo: String,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
        @Param("statuses") statuses: Collection<RegistrationStatus>,
    ): List<VisitorRegistration>

    /** 입차 이벤트 매칭용: 같은 단지, 같은 번호, 활성 상태, 시간 범위 포함 */
    @Query("""
        select r from VisitorRegistration r
        where r.household.apartment = :apartment
          and r.plateNo = :plateNo
          and r.status in :statuses
          and r.visitFrom <= :at and r.visitTo >= :at
        order by r.visitFrom desc
    """)
    fun findActiveForPlateAt(
        @Param("apartment") apartment: Apartment,
        @Param("plateNo") plateNo: String,
        @Param("at") at: LocalDateTime,
        @Param("statuses") statuses: Collection<RegistrationStatus>,
    ): List<VisitorRegistration>

    fun findByStatusInAndVisitToBefore(statuses: Collection<RegistrationStatus>, before: LocalDateTime): List<VisitorRegistration>
}

interface VisitHistoryRepository : JpaRepository<VisitHistory, Long> {
    fun findByHouseholdAndDeletedByUserFalseOrderByEnteredAtDesc(household: Household, pageable: Pageable): Page<VisitHistory>
    fun findByIdAndHousehold(id: Long, household: Household): VisitHistory?
    fun findFirstByRegistrationAndExitedAtIsNull(registration: VisitorRegistration): VisitHistory?

    @Query("""
        select h from VisitHistory h
        where h.household.apartment = :apartment and h.plateNo = :plateNo and h.exitedAt is null
        order by h.enteredAt desc
    """)
    fun findOpenByPlate(@Param("apartment") apartment: Apartment, @Param("plateNo") plateNo: String): List<VisitHistory>

    @Query("""
        select h from VisitHistory h
        where h.household = :household and h.enteredAt >= :from and h.enteredAt < :to
    """)
    fun findByHouseholdBetween(
        @Param("household") household: Household,
        @Param("from") from: LocalDateTime,
        @Param("to") to: LocalDateTime,
    ): List<VisitHistory>
}

interface SyncOutboxRepository : JpaRepository<SyncOutbox, Long> {
    fun findByApartmentAndStatusOrderByCreatedAtAsc(apartment: Apartment, status: SyncJobStatus, pageable: Pageable): List<SyncOutbox>
    fun findByIdAndApartment(id: Long, apartment: Apartment): SyncOutbox?
    fun findByStatusAndDeliveredAtBefore(status: SyncJobStatus, before: LocalDateTime): List<SyncOutbox>
}
