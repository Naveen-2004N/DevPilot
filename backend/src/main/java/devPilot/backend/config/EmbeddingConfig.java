package devPilot.backend.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import devPilot.backend.services.ai.ResilientEmbeddingModel;

@Configuration
public class EmbeddingConfig {

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String baseUrl;

    @Bean
    @Primary
    public EmbeddingModel embeddingModel(ObjectProvider<OpenAiEmbeddingModel> openAiEmbeddingModelProvider) {
        // Chat-only providers like Groq don't provide text embeddings; use local vectors directly
        if (baseUrl != null && baseUrl.contains("groq.com")) {
            return new ResilientEmbeddingModel(null);
        }
        return new ResilientEmbeddingModel(openAiEmbeddingModelProvider.getIfAvailable());
    }
}
