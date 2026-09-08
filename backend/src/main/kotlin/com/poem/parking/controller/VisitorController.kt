package com.poem.parking.controller

import com.poem.parking.config.CurrentUser
import com.poem.parking.dto.*
import com.poem.parking.service.HistoryService
import com.poem.parking.service.VisitorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/registrations")
class VisitorController(
    private val visitorService: VisitorService,
    private val currentUser: CurrentUser,
) {
    @PostMapping
    fun register(@Valid @RequestBody req: RegisterVisitorRequest): ResponseEntity<RegistrationResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(visitorService.register(currentUser.verifiedUser(), req))

    @GetMapping
    fun list(@RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) =
        visitorService.list(currentUser.verifiedUser(), page, size.coerceIn(1, 100))

    /** 현재 유효한(대기/반영/입차) 등록 */
    @GetMapping("/active")
    fun active(): List<RegistrationResponse> = visitorService.active(currentUser.verifiedUser())

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long) = visitorService.get(currentUser.verifiedUser(), id)

    @DeleteMapping("/{id}")
    fun cancel(@PathVariable id: Long) = visitorService.cancel(currentUser.verifiedUser(), id)
}

@RestController
@RequestMapping("/api/v1/histories")
class HistoryController(
    private val historyService: HistoryService,
    private val currentUser: CurrentUser,
) {
    @GetMapping
    fun list(@RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) =
        historyService.list(currentUser.verifiedUser(), page, size.coerceIn(1, 100))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        historyService.delete(currentUser.verifiedUser(), id)
        return ResponseEntity.noContent().build()
    }
}

@RestController
@RequestMapping("/api/v1/fees")
class FeeController(
    private val historyService: HistoryService,
    private val currentUser: CurrentUser,
) {
    /** 월별 요금 집계. year/month 생략 시 이번 달 */
    @GetMapping("/summary")
    fun summary(@RequestParam(required = false) year: Int?, @RequestParam(required = false) month: Int?): FeeSummaryResponse {
        val today = LocalDate.now()
        return historyService.monthlySummary(currentUser.verifiedUser(), year ?: today.year, month ?: today.monthValue)
    }
}
