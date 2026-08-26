package io.github.pedrovvitor.talentintelligence.adapter.persistence

import io.github.pedrovvitor.talentintelligence.application.SemanticJobCandidate
import io.github.pedrovvitor.talentintelligence.application.SemanticJobIndex
import io.github.pedrovvitor.talentintelligence.domain.TenantId
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class PgVectorJobIndex(
    private val jdbcClient: JdbcClient,
) : SemanticJobIndex {
    override fun index(
        tenantId: TenantId,
        jobId: UUID,
        searchableContent: String,
        embedding: FloatArray,
        embeddingModel: String,
    ) {
        jdbcClient.sql(
            """
            insert into job_embeddings (tenant_id, job_id, searchable_content, embedding, embedding_model)
            values (:tenantId, :jobId, :searchableContent, cast(:embedding as vector), :embeddingModel)
            on conflict (tenant_id, job_id) do update set
                searchable_content = excluded.searchable_content,
                embedding = excluded.embedding,
                embedding_model = excluded.embedding_model,
                updated_at = now()
            """.trimIndent(),
        )
            .param("tenantId", tenantId.value)
            .param("jobId", jobId)
            .param("searchableContent", searchableContent)
            .param("embedding", embedding.toVectorLiteral())
            .param("embeddingModel", embeddingModel)
            .update()
    }

    override fun search(
        tenantId: TenantId,
        queryEmbedding: FloatArray,
        embeddingModel: String,
        limit: Int,
    ): List<SemanticJobCandidate> = jdbcClient.sql(
        """
        select job_id, 1 - (embedding <=> cast(:queryEmbedding as vector)) as score
        from job_embeddings
        where tenant_id = :tenantId
          and embedding_model = :embeddingModel
        order by embedding <=> cast(:queryEmbedding as vector)
        limit :limit
        """.trimIndent(),
    )
        .param("tenantId", tenantId.value)
        .param("queryEmbedding", queryEmbedding.toVectorLiteral())
        .param("embeddingModel", embeddingModel)
        .param("limit", limit)
        .query { resultSet, _ ->
            SemanticJobCandidate(
                jobId = resultSet.getObject("job_id", UUID::class.java),
                score = resultSet.getDouble("score"),
            )
        }
        .list()

    private fun FloatArray.toVectorLiteral(): String = joinToString(prefix = "[", postfix = "]")

}
