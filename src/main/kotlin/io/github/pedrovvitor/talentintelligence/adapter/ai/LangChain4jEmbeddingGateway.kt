package io.github.pedrovvitor.talentintelligence.adapter.ai

import dev.langchain4j.model.embedding.EmbeddingModel
import io.github.pedrovvitor.talentintelligence.application.EmbeddingGateway
import org.springframework.stereotype.Component

@Component
class LangChain4jEmbeddingGateway(
    private val embeddingModel: EmbeddingModel,
) : EmbeddingGateway {
    override val modelVersion: String = "bge-small-en-v1.5-q"

    override fun embed(text: String): FloatArray = embeddingModel.embed(text).content().vector()
}
