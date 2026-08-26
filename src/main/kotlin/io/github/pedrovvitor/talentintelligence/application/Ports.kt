package io.github.pedrovvitor.talentintelligence.application

import io.github.pedrovvitor.talentintelligence.domain.CandidateProfile
import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import io.github.pedrovvitor.talentintelligence.domain.MatchDecision
import io.github.pedrovvitor.talentintelligence.domain.MatchDecisionRecord
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import java.util.UUID

interface JobCatalog {
    fun save(tenantId: TenantId, job: JobPosting): JobPosting
    fun findById(tenantId: TenantId, id: UUID): JobPosting?
    fun findAll(tenantId: TenantId): List<JobPosting>
    fun count(tenantId: TenantId): Long
}

interface EmbeddingGateway {
    val modelVersion: String
    fun embed(text: String): FloatArray
}

data class SemanticJobCandidate(
    val jobId: UUID,
    val score: Double,
)

interface SemanticJobIndex {
    fun index(
        tenantId: TenantId,
        jobId: UUID,
        searchableContent: String,
        embedding: FloatArray,
        embeddingModel: String,
    )

    fun search(
        tenantId: TenantId,
        queryEmbedding: FloatArray,
        embeddingModel: String,
        limit: Int,
    ): List<SemanticJobCandidate>
}

interface CandidateSourceFingerprinter {
    fun fingerprint(tenantId: TenantId, candidate: CandidateProfile): CandidateSourceFingerprint
}

data class CandidateSourceFingerprint(
    val value: String,
    val keyVersion: String,
)

interface MatchDecisionAudit {
    fun append(record: MatchDecisionRecord)
    fun findById(tenantId: TenantId, decisionId: UUID): MatchDecision?
}
