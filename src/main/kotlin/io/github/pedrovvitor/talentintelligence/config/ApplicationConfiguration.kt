package io.github.pedrovvitor.talentintelligence.config

import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel
import io.github.pedrovvitor.talentintelligence.domain.EligibilityPolicy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApplicationConfiguration {
    @Bean
    fun eligibilityPolicy(): EligibilityPolicy = EligibilityPolicy()

    @Bean
    fun embeddingModel(): EmbeddingModel = BgeSmallEnV15QuantizedEmbeddingModel()
}
