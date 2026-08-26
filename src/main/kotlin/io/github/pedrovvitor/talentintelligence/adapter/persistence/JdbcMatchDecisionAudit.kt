package io.github.pedrovvitor.talentintelligence.adapter.persistence

import io.github.pedrovvitor.talentintelligence.application.MatchDecisionAudit
import io.github.pedrovvitor.talentintelligence.domain.JobMatch
import io.github.pedrovvitor.talentintelligence.domain.MatchDecision
import io.github.pedrovvitor.talentintelligence.domain.MatchDecisionRecord
import io.github.pedrovvitor.talentintelligence.domain.MatchEvidence
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.UUID

@Repository
class JdbcMatchDecisionAudit(
    private val jdbcClient: JdbcClient,
) : MatchDecisionAudit {
    override fun append(record: MatchDecisionRecord) {
        val decision = record.decision
        jdbcClient.sql(
            """
            insert into match_decisions (
                id, tenant_id, actor_id, purpose, source_fingerprint, fingerprint_key_version, policy_version, embedding_model,
                generative_model, prompt_version, created_at
            ) values (
                :id, :tenantId, :actorId, :purpose, :sourceFingerprint, :fingerprintKeyVersion, :policyVersion, :embeddingModel,
                :generativeModel, :promptVersion, :createdAt
            )
            """.trimIndent(),
        )
            .param("id", decision.id)
            .param("tenantId", record.tenantId.value)
            .param("actorId", record.actorId)
            .param("purpose", record.purpose)
            .param("sourceFingerprint", record.sourceFingerprint)
            .param("fingerprintKeyVersion", record.fingerprintKeyVersion)
            .param("policyVersion", decision.policyVersion)
            .param("embeddingModel", decision.embeddingModel)
            .param("generativeModel", decision.generativeModel)
            .param("promptVersion", decision.promptVersion)
            .param("createdAt", Timestamp.from(decision.createdAt))
            .update()

        decision.matches.forEachIndexed { index, match ->
            val rank = index + 1
            appendResult(record.tenantId, decision.id, rank, match)
            match.evidence.forEachIndexed { evidenceIndex, evidence ->
                appendEvidence(record.tenantId, decision.id, rank, evidenceIndex + 1, evidence)
            }
        }
    }

    override fun findById(tenantId: TenantId, decisionId: UUID): MatchDecision? {
        val metadata = jdbcClient.sql(
            """
            select id, created_at, policy_version, embedding_model, generative_model, prompt_version
            from match_decisions
            where tenant_id = :tenantId and id = :decisionId
            """.trimIndent(),
        )
            .param("tenantId", tenantId.value)
            .param("decisionId", decisionId)
            .query(::mapMetadata)
            .optional()
            .orElse(null) ?: return null
        val evidenceByRank = findEvidence(tenantId, decisionId).groupBy(AuditedEvidence::rank)
        val matches = jdbcClient.sql(
            """
            select rank, job_id, title_snapshot, company_snapshot, semantic_score, skill_coverage, final_score
            from match_decision_results
            where tenant_id = :tenantId and decision_id = :decisionId
            order by rank
            """.trimIndent(),
        )
            .param("tenantId", tenantId.value)
            .param("decisionId", decisionId)
            .query { resultSet, _ -> mapMatch(resultSet, evidenceByRank[resultSet.getInt("rank")].orEmpty()) }
            .list()
        return metadata.copy(matches = matches)
    }

    private fun appendResult(tenantId: TenantId, decisionId: UUID, rank: Int, match: JobMatch) {
        jdbcClient.sql(
            """
            insert into match_decision_results (
                tenant_id, decision_id, rank, job_id, title_snapshot, company_snapshot,
                semantic_score, skill_coverage, final_score
            ) values (
                :tenantId, :decisionId, :rank, :jobId, :title, :company,
                :semanticScore, :skillCoverage, :finalScore
            )
            """.trimIndent(),
        )
            .param("tenantId", tenantId.value)
            .param("decisionId", decisionId)
            .param("rank", rank)
            .param("jobId", match.jobId)
            .param("title", match.title)
            .param("company", match.company)
            .param("semanticScore", match.semanticScore)
            .param("skillCoverage", match.skillCoverage)
            .param("finalScore", match.finalScore)
            .update()
    }

    private fun appendEvidence(
        tenantId: TenantId,
        decisionId: UUID,
        rank: Int,
        evidenceOrder: Int,
        evidence: MatchEvidence,
    ) {
        jdbcClient.sql(
            """
            insert into match_decision_evidence (
                tenant_id, decision_id, result_rank, evidence_order, evidence_type, label, evidence_value
            ) values (
                :tenantId, :decisionId, :resultRank, :evidenceOrder, :evidenceType, :label, :evidenceValue
            )
            """.trimIndent(),
        )
            .param("tenantId", tenantId.value)
            .param("decisionId", decisionId)
            .param("resultRank", rank)
            .param("evidenceOrder", evidenceOrder)
            .param("evidenceType", evidence.type)
            .param("label", evidence.label)
            .param("evidenceValue", evidence.value)
            .update()
    }

    private fun findEvidence(tenantId: TenantId, decisionId: UUID): List<AuditedEvidence> = jdbcClient.sql(
        """
        select result_rank, evidence_type, label, evidence_value
        from match_decision_evidence
        where tenant_id = :tenantId and decision_id = :decisionId
        order by result_rank, evidence_order
        """.trimIndent(),
    )
        .param("tenantId", tenantId.value)
        .param("decisionId", decisionId)
        .query { resultSet, _ ->
            AuditedEvidence(
                rank = resultSet.getInt("result_rank"),
                evidence = MatchEvidence(
                    type = resultSet.getString("evidence_type"),
                    label = resultSet.getString("label"),
                    value = resultSet.getString("evidence_value"),
                ),
            )
        }
        .list()

    private fun mapMetadata(resultSet: ResultSet, rowNumber: Int): MatchDecision = MatchDecision(
        id = resultSet.getObject("id", UUID::class.java),
        createdAt = resultSet.getTimestamp("created_at").toInstant(),
        policyVersion = resultSet.getString("policy_version"),
        embeddingModel = resultSet.getString("embedding_model"),
        generativeModel = resultSet.getString("generative_model"),
        promptVersion = resultSet.getString("prompt_version"),
        matches = emptyList(),
    )

    private fun mapMatch(resultSet: ResultSet, evidence: List<AuditedEvidence>): JobMatch = JobMatch(
        jobId = resultSet.getObject("job_id", UUID::class.java),
        title = resultSet.getString("title_snapshot"),
        company = resultSet.getString("company_snapshot"),
        semanticScore = resultSet.getDouble("semantic_score"),
        skillCoverage = resultSet.getDouble("skill_coverage"),
        finalScore = resultSet.getDouble("final_score"),
        evidence = evidence.map(AuditedEvidence::evidence),
    )
}

private data class AuditedEvidence(
    val rank: Int,
    val evidence: MatchEvidence,
)
