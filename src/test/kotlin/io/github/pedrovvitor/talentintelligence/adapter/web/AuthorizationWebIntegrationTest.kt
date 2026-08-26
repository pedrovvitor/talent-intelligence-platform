package io.github.pedrovvitor.talentintelligence.adapter.web

import io.github.pedrovvitor.talentintelligence.application.JobCatalogService
import io.github.pedrovvitor.talentintelligence.application.MatchingService
import io.github.pedrovvitor.talentintelligence.application.RequestIdentity
import io.github.pedrovvitor.talentintelligence.config.SecurityConfiguration
import io.github.pedrovvitor.talentintelligence.domain.CandidateProfile
import io.github.pedrovvitor.talentintelligence.domain.MatchDecision
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.HttpHeaders
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.util.UUID

@WebMvcTest(controllers = [JobController::class, MatchController::class])
@Import(SecurityConfiguration::class, RequestIdentityResolver::class, SyntheticJwtConfiguration::class)
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
    fun `token with invalid signature fails closed`() {
        val token = SyntheticJwtFixture.invalidSignatureToken(TENANT_ID, listOf("recruiter"))

        mockMvc.get("/api/jobs") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTHENTICATION_REQUIRED") }
        }
    }

    @Test
    fun `expired signed token fails closed`() {
        val token = SyntheticJwtFixture.validToken(
            TENANT_ID,
            listOf("recruiter"),
            expiresAt = Instant.now().minusSeconds(120),
        )

        mockMvc.get("/api/jobs") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTHENTICATION_REQUIRED") }
        }
    }

    @Test
    fun `signed token with wrong role is forbidden`() {
        val token = SyntheticJwtFixture.validToken(TENANT_ID, listOf("viewer"))

        mockMvc.get("/api/jobs") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("ACCESS_DENIED") }
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
        doReturn(EMPTY_DECISION).`when`(matchingService).match(
            RequestIdentity("user", TenantId.parse(TENANT_ID)),
            MATCH_CANDIDATE,
            5,
        )

        mockMvc.post("/api/matches") {
            with(
                jwt()
                    .jwt { token -> token.claim("tenant_id", TENANT_ID) }
                    .authorities(SimpleGrantedAuthority("ROLE_RECRUITER")),
            )
            contentType = MediaType.APPLICATION_JSON
            content = MATCH_REQUEST
        }.andExpect {
            status { isOk() }
            jsonPath("$.decisionId") { value(EMPTY_DECISION.id.toString()) }
            jsonPath("$.matches") { isEmpty() }
        }
    }

    @Test
    fun `recruiter can reproduce a tenant scoped audited decision`() {
        doReturn(EMPTY_DECISION).`when`(matchingService).findDecision(
            TenantId.parse(TENANT_ID),
            EMPTY_DECISION.id,
        )

        mockMvc.get("/api/matches/${EMPTY_DECISION.id}") {
            with(
                jwt()
                    .jwt { token -> token.claim("tenant_id", TENANT_ID) }
                    .authorities(SimpleGrantedAuthority("ROLE_RECRUITER")),
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.decisionId") { value(EMPTY_DECISION.id.toString()) }
            jsonPath("$.policyVersion") { value(EMPTY_DECISION.policyVersion) }
            jsonPath("$.embeddingModel") { value(EMPTY_DECISION.embeddingModel) }
        }
    }

    @Test
    fun `tenant cannot reproduce another tenant decision`() {
        val token = SyntheticJwtFixture.validToken(SECOND_TENANT_ID, listOf("recruiter"))

        mockMvc.get("/api/matches/${EMPTY_DECISION.id}") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("MATCH_DECISION_NOT_FOUND") }
        }
        verify(matchingService).findDecision(TenantId.parse(SECOND_TENANT_ID), EMPTY_DECISION.id)
    }

    @Test
    fun `recruiter cannot create jobs`() {
        mockMvc.post("/api/jobs") {
            with(
                jwt()
                    .jwt { token -> token.claim("tenant_id", TENANT_ID) }
                    .authorities(SimpleGrantedAuthority("ROLE_RECRUITER")),
            )
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
            with(
                jwt()
                    .jwt { token -> token.claim("tenant_id", TENANT_ID) }
                    .authorities(SimpleGrantedAuthority("ROLE_ADMIN")),
            )
            contentType = MediaType.APPLICATION_JSON
            content = INVALID_JOB_REQUEST
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }
    }

    companion object {
        private const val TENANT_ID = "00000000-0000-0000-0000-000000000001"
        private const val SECOND_TENANT_ID = "00000000-0000-0000-0000-000000000002"
        private val EMPTY_DECISION = MatchDecision(
            id = UUID.fromString("00000000-0000-0000-0000-000000000100"),
            createdAt = Instant.parse("2026-08-25T12:00:00Z"),
            policyVersion = "eligibility-policy-v1",
            embeddingModel = "synthetic-embedding-v1",
            generativeModel = null,
            promptVersion = null,
            matches = emptyList(),
        )
        private val MATCH_CANDIDATE = CandidateProfile(
            headline = "Synthetic JVM Engineer",
            summary = "Builds reliable services",
            skills = setOf("Kotlin"),
            seniority = Seniority.SENIOR,
            preferredWorkModes = setOf(WorkMode.REMOTE),
            preferredLocations = emptySet(),
            minimumSalary = null,
        )

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
