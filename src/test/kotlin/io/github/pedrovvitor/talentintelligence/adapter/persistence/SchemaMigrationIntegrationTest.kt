package io.github.pedrovvitor.talentintelligence.adapter.persistence

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class SchemaMigrationIntegrationTest {
    @Test
    fun `migration creates transactional and vector structures`() {
        Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .load()
            .migrate()

        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT extversion FROM pg_extension WHERE extname = 'vector'").use { result ->
                    assertTrue(result.next())
                }
                statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('tenants', 'jobs', 'job_embeddings')").use { result ->
                    assertTrue(result.next())
                    assertEquals(3, result.getInt(1))
                }
                statement.executeQuery(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_name IN ('jobs', 'job_embeddings') AND column_name = 'tenant_id'",
                ).use { result ->
                    assertTrue(result.next())
                    assertEquals(2, result.getInt(1))
                }
            }
        }
    }

    companion object {
        @Container
        @JvmStatic
        private val postgres = PostgreSQLContainer("pgvector/pgvector:pg17")
            .withDatabaseName("talent_intelligence_test")
            .withUsername("talent_test")
            .withPassword("talent_test")
    }
}
