package io.github.pedrovvitor.talentintelligence.adapter.persistence

import io.github.pedrovvitor.talentintelligence.application.JobCatalog
import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import io.github.pedrovvitor.talentintelligence.domain.Seniority
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import io.github.pedrovvitor.talentintelligence.domain.WorkMode
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.UUID

@Repository
class JdbcJobCatalog(
    private val jdbcClient: JdbcClient,
) : JobCatalog {
    override fun save(tenantId: TenantId, job: JobPosting): JobPosting {
        jdbcClient.sql(
            """
            insert into jobs (
                tenant_id, id, title, company, description, required_skills, seniority,
                work_mode, location, salary_min, salary_max
            ) values (
                :tenantId, :id, :title, :company, :description, :requiredSkills, :seniority,
                :workMode, :location, :salaryMin, :salaryMax
            )
            on conflict (tenant_id, id) do update set
                title = excluded.title,
                company = excluded.company,
                description = excluded.description,
                required_skills = excluded.required_skills,
                seniority = excluded.seniority,
                work_mode = excluded.work_mode,
                location = excluded.location,
                salary_min = excluded.salary_min,
                salary_max = excluded.salary_max,
                updated_at = now()
            """.trimIndent(),
        )
            .param("tenantId", tenantId.value)
            .param("id", job.id)
            .param("title", job.title)
            .param("company", job.company)
            .param("description", job.description)
            .param("requiredSkills", job.requiredSkills.joinToString(SKILL_SEPARATOR))
            .param("seniority", job.seniority.name)
            .param("workMode", job.workMode.name)
            .param("location", job.location)
            .param("salaryMin", job.salaryMin)
            .param("salaryMax", job.salaryMax)
            .update()
        return job
    }

    override fun findById(tenantId: TenantId, id: UUID): JobPosting? = jdbcClient
        .sql("select * from jobs where tenant_id = :tenantId and id = :id")
        .param("tenantId", tenantId.value)
        .param("id", id)
        .query(::mapJob)
        .optional()
        .orElse(null)

    override fun findAll(tenantId: TenantId): List<JobPosting> = jdbcClient
        .sql("select * from jobs where tenant_id = :tenantId order by created_at desc")
        .param("tenantId", tenantId.value)
        .query(::mapJob)
        .list()

    override fun count(tenantId: TenantId): Long = jdbcClient
        .sql("select count(*) from jobs where tenant_id = :tenantId")
        .param("tenantId", tenantId.value)
        .query(Long::class.java)
        .single()

    private fun mapJob(resultSet: ResultSet, rowNumber: Int): JobPosting = JobPosting(
        id = resultSet.getObject("id", UUID::class.java),
        title = resultSet.getString("title"),
        company = resultSet.getString("company"),
        description = resultSet.getString("description"),
        requiredSkills = resultSet.getString("required_skills")
            .split(SKILL_SEPARATOR)
            .filter(String::isNotBlank)
            .toSet(),
        seniority = Seniority.valueOf(resultSet.getString("seniority")),
        workMode = WorkMode.valueOf(resultSet.getString("work_mode")),
        location = resultSet.getString("location"),
        salaryMin = resultSet.getBigDecimal("salary_min"),
        salaryMax = resultSet.getBigDecimal("salary_max"),
    )

    companion object {
        private const val SKILL_SEPARATOR = "|"
    }
}
