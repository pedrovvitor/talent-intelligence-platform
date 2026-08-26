package io.github.pedrovvitor.talentintelligence.application

import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import java.util.UUID

interface JobCatalog {
    fun save(tenantId: TenantId, job: JobPosting): JobPosting
    fun findById(tenantId: TenantId, id: UUID): JobPosting?
    fun findAll(tenantId: TenantId): List<JobPosting>
    fun count(tenantId: TenantId): Long
}

interface EmbeddingGateway {
    fun embed(text: String): FloatArray
}

data class SemanticJobCandidate(
    val jobId: UUID,
    val score: Double,
)

interface SemanticJobIndex {
    fun index(tenantId: TenantId, jobId: UUID, searchableContent: String, embedding: FloatArray)
    fun search(tenantId: TenantId, queryEmbedding: FloatArray, limit: Int): List<SemanticJobCandidate>
}
