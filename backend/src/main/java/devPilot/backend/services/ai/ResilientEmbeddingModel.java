package devPilot.backend.services.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;

import lombok.extern.slf4j.Slf4j;

/**
 * Resilient Embedding Model:
 * Attempts to use the primary configured model (e.g., OpenAI / Gemini / GitHub Models).
 * If the provider returns 429 (no credits), 401, or any API failure, it seamlessly
 * falls back to a deterministic local embedding so that repository indexing
 * NEVER fails and remains 100% free.
 */
@Slf4j
public class ResilientEmbeddingModel implements EmbeddingModel {

    public static final int DIMENSIONS = 1536;

    private final OpenAiEmbeddingModel delegate;
    private volatile boolean fallbackActive = false;

    public ResilientEmbeddingModel(OpenAiEmbeddingModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        if (delegate != null && !fallbackActive) {
            try {
                return delegate.call(request);
            } catch (Exception ex) {
                log.warn("Primary embedding API failed ({}: {}). Switching to free local embeddings for indexing.",
                        ex.getClass().getSimpleName(), ex.getMessage());
                fallbackActive = true;
            }
        }

        List<Embedding> embeddings = new ArrayList<>();
        List<String> instructions = request.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            float[] vector = computeLocalVector(instructions.get(i));
            embeddings.add(new Embedding(vector, i));
        }

        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        if (delegate != null && !fallbackActive) {
            try {
                return delegate.embed(document);
            } catch (Exception ex) {
                log.warn("Primary embedding API failed for document: {}. Using free local embedding.", ex.getMessage());
                fallbackActive = true;
            }
        }
        return computeLocalVector(document.getText());
    }

    @Override
    public float[] embed(String text) {
        if (delegate != null && !fallbackActive) {
            try {
                return delegate.embed(text);
            } catch (Exception ex) {
                log.warn("Primary embedding API failed for text: {}. Using free local embedding.", ex.getMessage());
                fallbackActive = true;
            }
        }
        return computeLocalVector(text);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (delegate != null && !fallbackActive) {
            try {
                return delegate.embed(texts);
            } catch (Exception ex) {
                log.warn("Primary embedding API failed for batch: {}. Using free local embeddings.", ex.getMessage());
                fallbackActive = true;
            }
        }

        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(computeLocalVector(text));
        }
        return results;
    }

    /**
     * Deterministic, normalized feature-hashed vector of size 1536.
     */
    private float[] computeLocalVector(String text) {
        float[] vector = new float[DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String[] tokens = text.toLowerCase().split("[\\s\\p{Punct}]+");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.isEmpty()) continue;

            int h1 = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[h1] += 1.0f;

            int h2 = Math.floorMod((token + "_salt").hashCode(), DIMENSIONS);
            vector[h2] += 0.5f;

            if (i < tokens.length - 1 && !tokens[i + 1].isEmpty()) {
                int h3 = Math.floorMod((token + " " + tokens[i + 1]).hashCode(), DIMENSIONS);
                vector[h3] += 0.8f;
            }
        }

        // L2 normalize
        double sumSq = 0.0;
        for (float val : vector) {
            sumSq += val * val;
        }

        if (sumSq > 0.0) {
            float norm = (float) Math.sqrt(sumSq);
            for (int i = 0; i < DIMENSIONS; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }
}
