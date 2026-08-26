package io.github.pedrovvitor.talentintelligence.adapter.persistence

import org.flywaydb.core.Flyway
import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@Testcontainers(disabledWithoutDocker = true)
class TenantIsolationIntegrationTest {
    @Test
    fun `catalog and vector queries cannot cross tenant boundaries`() {
        val tenantAJob = job("Tenant A Kotlin Engineer")
        val tenantBJob = job("Tenant B Kotlin Engineer")
        catalog.save(TENANT_A, tenantAJob)
        catalog.save(TENANT_B, tenantBJob)
        vectorIndex.index(TENANT_A, tenantAJob.id, tenantAJob.title, unitVector())

        assertEquals(listOf(tenantAJob.id), catalog.findAll(TENANT_A).map(JobPosting::id))
        assertEquals(listOf(tenantBJob.id), catalog.findAll(TENANT_B).map(JobPosting::id))
        assertNull(catalog.findById(TENANT_A, tenantBJob.id))
        assertEquals(listOf(tenantAJob.id), vectorIndex.search(TENANT_A, unitVector(), 10).map { it.jobId })

        assertFailsWith<DataIntegrityViolationException> {
            jdbcClient.sql(
                """
                insert into job_embeddings (tenant_id, job_id, searchable_content, embedding, embedding_model)
                values (:tenantId, :jobId, :content, cast(:embedding as vector), :model)
                """.trimIndent(),
            )
                .param("tenantId", TENANT_A.value)
                .param("jobId", tenantBJob.id)
                .param("content", tenantBJob.title)
                .param("embedding", unitVector().joinToString(prefix = "[", postfix = "]"))
                .param("model", PgVectorJobIndex.EMBEDDING_MODEL)
                .update()
        }
    }

    private fun job(title: String): JobPosting = JobPosting(
        id = UUID.randomUUID(),
        title = title,
        company = "Synthetic Tenant",
        description = "Tenant isolation integration fixture",
        requiredSkills = setOf("Kotlin"),
        seniority = Seniority.SENIOR,
        workMode = WorkMode.REMOTE,
        location = null,
        salaryMin = BigDecimal("100000"),
        salaryMax = BigDecimal("130000"),
    )

    private fun unitVector(): FloatArray = FloatArray(384).also { vector -> vector[0] = 1f }

    companion object {
        private val TENANT_A = TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        private val TENANT_B = TenantId(UUID.fromString("00000000-0000-0000-0000-000000000002"))

        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("talent_tenant_test")
            .withUsername("talent_test")
            .withPassword("talent_test")

        private lateinit var jdbcClient: JdbcClient
        private lateinit var catalog: JdbcJobCatalog
        private lateinit var vectorIndex: PgVectorJobIndex

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
                "insert into tenants (id, slug, display_name) values (:id, 'tenant-b', 'Synthetic Tenant B')",
            )
                .param("id", TENANT_B.value)
                .update()
            catalog = JdbcJobCatalog(jdbcClient)
            vectorIndex = PgVectorJobIndex(jdbcClient)
        }
    }
}
