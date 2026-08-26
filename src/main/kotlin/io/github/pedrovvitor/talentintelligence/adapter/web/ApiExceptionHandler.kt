package io.github.pedrovvitor.talentintelligence.adapter.web

import io.github.pedrovvitor.talentintelligence.application.InvalidJobException
import io.github.pedrovvitor.talentintelligence.application.MatchDecisionNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ApiError(
    val timestamp: Instant,
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val violations: Map<String, String> = emptyMap(),
)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> {
        val violations = exception.bindingResult.fieldErrors.associate { error ->
            error.field to ValidationMessageRedactor.redact(error.codes.orEmpty())
        }
        return errorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", request, violations)
    }

    @ExceptionHandler(InvalidJobException::class)
    fun handleInvalidJob(exception: InvalidJobException, request: HttpServletRequest): ResponseEntity<ApiError> =
        errorResponse(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_JOB", exception.message ?: "Invalid job", request)

    @ExceptionHandler(MatchDecisionNotFoundException::class)
    fun handleMissingDecision(
        exception: MatchDecisionNotFoundException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiError> =
        errorResponse(HttpStatus.NOT_FOUND, "MATCH_DECISION_NOT_FOUND", exception.message ?: "Match decision was not found", request)

    private fun errorResponse(
        status: HttpStatus,
        code: String,
        message: String,
        request: HttpServletRequest,
        violations: Map<String, String> = emptyMap(),
    ): ResponseEntity<ApiError> = ResponseEntity.status(status).body(
        ApiError(
            timestamp = Instant.now(),
            status = status.value(),
            code = code,
            message = message,
            path = request.requestURI,
            violations = violations,
        ),
    )
}

private object ValidationMessageRedactor {
    private val messagesByConstraint = mapOf(
        "NotBlank" to "Required value is missing",
        "NotEmpty" to "Required value is missing",
        "Size" to "Value has an invalid size",
        "Pattern" to "Value has an invalid format",
        "DecimalMin" to "Value is outside the allowed range",
        "Min" to "Value is outside the allowed range",
        "Max" to "Value is outside the allowed range",
    )

    fun redact(codes: Array<out String>): String = codes
        .asSequence()
        .map { code -> code.substringBefore('.') }
        .mapNotNull(messagesByConstraint::get)
        .firstOrNull()
        ?: "Invalid value"
}
