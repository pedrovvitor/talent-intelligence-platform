package io.github.pedrovvitor.talentintelligence.adapter.web

import io.github.pedrovvitor.talentintelligence.application.JobCatalogService
import io.github.pedrovvitor.talentintelligence.application.MatchingService
import io.github.pedrovvitor.talentintelligence.config.SecurityConfiguration
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@WebMvcTest(controllers = [JobController::class, MatchController::class])
@Import(SecurityConfiguration::class)
class AuthorizationWebIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var jobCatalogService: JobCatalogService

    @MockitoBean
    private lateinit var matchingService: MatchingService

    @Test
    fun `missing bearer token fails closed`() {
        mockMvc.get("/api/jobs")
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("AUTHENTICATION_REQUIRED") }
            }
    }

    @Test
    fun `authenticated user without platform role is forbidden`() {
        mockMvc.get("/api/jobs") {
            with(jwt().jwt { token -> token.subject("synthetic-user") })
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("ACCESS_DENIED") }
        }
    }

    @Test
    fun `recruiter can request matches`() {
        mockMvc.post("/api/matches") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_RECRUITER")))
            contentType = MediaType.APPLICATION_JSON
            content = MATCH_REQUEST
        }.andExpect {
            status { isOk() }
            jsonPath("$.matches") { isEmpty() }
        }
    }

    @Test
    fun `recruiter cannot create jobs`() {
        mockMvc.post("/api/jobs") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_RECRUITER")))
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("ACCESS_DENIED") }
        }
    }

    @Test
    fun `admin reaches catalog mutation validation`() {
        mockMvc.post("/api/jobs") {
            with(jwt().authorities(SimpleGrantedAuthority("ROLE_ADMIN")))
            contentType = MediaType.APPLICATION_JSON
            content = INVALID_JOB_REQUEST
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }
    }

    companion object {
        private val MATCH_REQUEST = """
            {
              "headline": "Synthetic JVM Engineer",
              "summary": "Builds reliable services",
              "skills": ["Kotlin"],
              "seniority": "SENIOR",
              "preferredWorkModes": ["REMOTE"],
              "preferredLocations": [],
              "minimumSalary": null,
              "limit": 5
            }
        """.trimIndent()

        private val INVALID_JOB_REQUEST = """
            {
              "title": "",
              "company": "",
              "description": "",
              "requiredSkills": [],
              "seniority": "SENIOR",
              "workMode": "REMOTE",
              "location": null,
              "salaryMin": null,
              "salaryMax": null
            }
        """.trimIndent()
    }
}
