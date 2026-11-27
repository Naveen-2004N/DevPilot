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
        List<Document> documents = List.of();

        // 1. Try vector similarity search with metadata filter
        try {
            var filter = new FilterExpressionBuilder()
                    .eq(RagSettings.METADATA_REPO_ID, repositoryId.toString())
                    .build();

            var search = SearchRequest.builder()
                    .query(question)
                    .topK(RagSettings.TOP_K_CHUNKS)
                    .filterExpression(filter)
                    .build();

            documents = vectorStore.similaritySearch(search);
        } catch (Exception ex) {
            log.warn("Filtered vector search failed ({}). Falling back to search with in-memory filter...", ex.getMessage());
            // 2. If Qdrant requires payload index for 'repoId', search without filter and filter in-memory
            try {
                var search = SearchRequest.builder()
                        .query(question)
                        .topK(RagSettings.TOP_K_CHUNKS * 2)
                        .build();

                List<Document> rawDocs = vectorStore.similaritySearch(search);
                if (rawDocs != null && !rawDocs.isEmpty()) {
                    List<Document> matched = rawDocs.stream()
                            .filter(doc -> {
                                Object rId = doc.getMetadata().get(RagSettings.METADATA_REPO_ID);
                                return rId == null || repositoryId.toString().equalsIgnoreCase(String.valueOf(rId));
                            })
                            .toList();
                    documents = matched.isEmpty() ? rawDocs : matched;
                }
            } catch (Exception innerEx) {
                log.warn("Vector search failed completely: {}. Continuing without vector store matches.", innerEx.getMessage());
            }
        }

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
    }
}