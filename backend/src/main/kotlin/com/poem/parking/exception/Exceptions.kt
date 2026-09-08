package com.poem.parking.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

open class ApiException(val status: HttpStatus, val code: String, message: String) : RuntimeException(message)
class NotFoundException(message: String) : ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message)
class BadRequestException(code: String, message: String) : ApiException(HttpStatus.BAD_REQUEST, code, message)
class ConflictException(code: String, message: String) : ApiException(HttpStatus.CONFLICT, code, message)
class UnauthorizedException(message: String) : ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message)
class ForbiddenException(message: String) : ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message)

data class ErrorResponse(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val details: Map<String, String?>? = null,
)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ApiException::class)
    fun handleApi(e: ApiException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(e.status).body(ErrorResponse(e.code, e.message ?: ""))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val details = e.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        return ResponseEntity.badRequest().body(ErrorResponse("VALIDATION_ERROR", "입력값이 올바르지 않습니다.", details = details))
    }

    @ExceptionHandler(Exception::class)
    fun handleOther(e: Exception): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("INTERNAL_ERROR", e.message ?: "서버 오류"))
}
