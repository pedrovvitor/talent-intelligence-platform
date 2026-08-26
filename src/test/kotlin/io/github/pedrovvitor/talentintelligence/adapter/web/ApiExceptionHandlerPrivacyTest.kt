package io.github.pedrovvitor.talentintelligence.adapter.web

import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ApiExceptionHandlerPrivacyTest {
    @Test
    fun `validation response does not disclose rejected candidate data`() {
        val candidateEmail = "candidate@example.test"
        val bindingResult = BeanPropertyBindingResult(TestRequest(candidateEmail), "request")
        bindingResult.addError(
            FieldError(
                "request",
                "summary",
                candidateEmail,
                false,
                arrayOf("UnsafeConstraint.request.summary"),
                null,
                "Rejected value was $candidateEmail",
            ),
        )
        val exception = MethodArgumentNotValidException(methodParameter(), bindingResult)

        val response = ApiExceptionHandler().handleValidation(exception, MockHttpServletRequest("POST", "/api/matches"))

        assertEquals("Invalid value", response.body?.violations?.get("summary"))
        assertFalse(response.body.toString().contains(candidateEmail))
    }

    @Test
    fun `validation response maps allow-listed constraint to stable message`() {
        val bindingResult = BeanPropertyBindingResult(TestRequest(""), "request")
        bindingResult.addError(
            FieldError(
                "request",
                "summary",
                "",
                false,
                arrayOf("NotBlank.request.summary", "NotBlank.summary", "NotBlank"),
                null,
                "must not be blank",
            ),
        )
        val exception = MethodArgumentNotValidException(methodParameter(), bindingResult)

        val response = ApiExceptionHandler().handleValidation(exception, MockHttpServletRequest("POST", "/api/matches"))

        assertEquals("Required value is missing", response.body?.violations?.get("summary"))
    }

    private fun methodParameter(): MethodParameter = MethodParameter(
        PrivacyTestController::class.java.getDeclaredMethod("submit", TestRequest::class.java),
        0,
    )
}

private data class TestRequest(val summary: String)

private class PrivacyTestController {
    fun submit(request: TestRequest) = request
}
