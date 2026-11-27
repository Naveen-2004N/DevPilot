package devPilot.backend.services.ai;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeContextRetriever {
    private static final String NO_MATCHES = "(no matching code chunks found)";

    private final VectorStore vectorStore;
    private final CitationMapper citationMapper; 

    public RetrievedContext retrieve(UUID repositoryId, String question) {
        try {
            var filter = new FilterExpressionBuilder()
                    .eq(RagSettings.METADATA_REPO_ID, repositoryId.toString())
                    .build();

            var search = SearchRequest.builder()
                    .query(question)
                    .topK(RagSettings.TOP_K_CHUNKS)
                    .filterExpression(filter)
                    .build();

            var documents = vectorStore.similaritySearch(search);

            if (documents == null || documents.isEmpty()) {
                return new RetrievedContext(List.of(), NO_MATCHES);
            }

            var citations = documents.stream()
                    .map(citationMapper::fromDocument)
                    .distinct()
                    .toList();

            var contextText = documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n---\n\n"));

            if (contextText.isBlank()) {
                contextText = NO_MATCHES;
            }

            return new RetrievedContext(citations, contextText);
        } catch (Exception ex) {
            log.warn("Vector search failed for repo {}: {}. Continuing without vector store matches.",
                    repositoryId, ex.getMessage());
            return new RetrievedContext(List.of(), NO_MATCHES);
        }
    }
}