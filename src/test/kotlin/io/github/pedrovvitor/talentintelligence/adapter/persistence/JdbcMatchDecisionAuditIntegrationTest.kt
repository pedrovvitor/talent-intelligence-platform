package io.github.pedrovvitor.talentintelligence.adapter.persistence

import io.github.pedrovvitor.talentintelligence.domain.JobMatch
import io.github.pedrovvitor.talentintelligence.domain.MatchDecision
import io.github.pedrovvitor.talentintelligence.domain.MatchDecisionRecord
import io.github.pedrovvitor.talentintelligence.domain.MatchEvidence
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@Testcontainers(disabledWithoutDocker = true)
class JdbcMatchDecisionAuditIntegrationTest {
    @Test
    fun `audit round trip preserves the displayed decision and rejects updates`() {
        val record = record()

        audit.append(record)

        assertEquals(record.decision, audit.findById(TENANT_A, record.decision.id))
        assertNull(audit.findById(TENANT_B, record.decision.id))
        assertFailsWith<DataAccessException> {
            jdbcClient.sql("update match_decisions set policy_version = 'rewritten' where id = :id")
                .param("id", record.decision.id)
                .update()
        }
        val persistedFingerprint = jdbcClient.sql("select source_fingerprint from match_decisions where id = :id")
            .param("id", record.decision.id)
            .query(String::class.java)
            .single()
        assertEquals(SOURCE_FINGERPRINT, persistedFingerprint.trim())
    }

    private fun record(): MatchDecisionRecord {
        val decision = MatchDecision(
            id = UUID.randomUUID(),
            createdAt = Instant.parse("2026-08-25T12:00:00Z"),
            policyVersion = "eligibility-policy-v1",
            embeddingModel = "synthetic-embedding-v1",
            generativeModel = null,
            promptVersion = null,
            matches = listOf(
                JobMatch(
                    jobId = UUID.randomUUID(),
                    title = "Synthetic Kotlin Engineer",
                    company = "Synthetic Company",
                    semanticScore = 0.82,
                    skillCoverage = 1.0,
                    finalScore = 0.874,
                    evidence = listOf(
                        MatchEvidence("semantic", "Semantic relevance", "82%"),
                        MatchEvidence("eligibility", "Hard constraints", "Passed"),
                    ),
                ),
            ),
        )
        return MatchDecisionRecord(
            decision,
            TENANT_A,
            "synthetic-actor",
            "candidate-job-matching",
            SOURCE_FINGERPRINT,
            "synthetic-key-v1",
        )
    }

    companion object {
        private val TENANT_A = TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        private val TENANT_B = TenantId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
        private const val SOURCE_FINGERPRINT = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("talent_audit_test")
            .withUsername("talent_test")
            .withPassword("talent_test")

        private lateinit var jdbcClient: JdbcClient
        private lateinit var audit: JdbcMatchDecisionAudit

        @BeforeAll
        @JvmStatic
        fun initializeDatabase() {
            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .load()
                .migrate()
            val dataSource = DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            jdbcClient = JdbcClient.create(dataSource)
            jdbcClient.sql(
                "insert into tenants (id, slug, display_name) values (:id, 'audit-tenant-b', 'Synthetic Audit Tenant B')",
            )
                .param("id", TENANT_B.value)
                .update()
            audit = JdbcMatchDecisionAudit(jdbcClient)
        }
    }
}
