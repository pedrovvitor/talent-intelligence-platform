package io.github.pedrovvitor.talentintelligence.application

import io.github.pedrovvitor.talentintelligence.domain.JobPosting
import java.util.UUID

interface JobCatalog {
    fun save(job: JobPosting): JobPosting
    fun findById(id: UUID): JobPosting?
    fun findAll(): List<JobPosting>
    fun count(): Long
}

interface EmbeddingGateway {
    fun embed(text: String): FloatArray
}

data class SemanticJobCandidate(
    val jobId: UUID,
    val score: Double,
)

interface SemanticJobIndex {
    fun index(jobId: UUID, searchableContent: String, embedding: FloatArray)
    fun search(queryEmbedding: FloatArray, limit: Int): List<SemanticJobCandidate>
}
