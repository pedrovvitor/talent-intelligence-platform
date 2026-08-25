package io.github.pedrovvitor.talentintelligence.adapter.persistence

import io.github.pedrovvitor.talentintelligence.application.SemanticJobCandidate
import io.github.pedrovvitor.talentintelligence.application.SemanticJobIndex
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PgVectorJobIndex(
    private val jdbcClient: JdbcClient,
) : SemanticJobIndex {
    override fun index(jobId: UUID, searchableContent: String, embedding: FloatArray) {
        jdbcClient.sql(
            """
            insert into job_embeddings (job_id, searchable_content, embedding, embedding_model)
            values (:jobId, :searchableContent, cast(:embedding as vector), :embeddingModel)
            on conflict (job_id) do update set
                searchable_content = excluded.searchable_content,
                embedding = excluded.embedding,
                embedding_model = excluded.embedding_model,
                updated_at = now()
            """.trimIndent(),
        )
            .param("jobId", jobId)
            .param("searchableContent", searchableContent)
            .param("embedding", embedding.toVectorLiteral())
            .param("embeddingModel", EMBEDDING_MODEL)
            .update()
    }

    override fun search(queryEmbedding: FloatArray, limit: Int): List<SemanticJobCandidate> = jdbcClient.sql(
        """
        select job_id, 1 - (embedding <=> cast(:queryEmbedding as vector)) as score
        from job_embeddings
        where embedding_model = :embeddingModel
        order by embedding <=> cast(:queryEmbedding as vector)
        limit :limit
        """.trimIndent(),
    )
        .param("queryEmbedding", queryEmbedding.toVectorLiteral())
        .param("embeddingModel", EMBEDDING_MODEL)
        .param("limit", limit)
        .query { resultSet, _ ->
            SemanticJobCandidate(
                jobId = resultSet.getObject("job_id", UUID::class.java),
                score = resultSet.getDouble("score"),
            )
        }
        .list()

    private fun FloatArray.toVectorLiteral(): String = joinToString(prefix = "[", postfix = "]")

    companion object {
        const val EMBEDDING_MODEL = "bge-small-en-v1.5-q"
    }
}
