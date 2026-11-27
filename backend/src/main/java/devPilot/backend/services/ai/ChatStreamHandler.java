package devPilot.backend.services.ai;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import devPilot.backend.dto.ChatMessageResponse;
import devPilot.backend.dto.CitationDto;
import devPilot.backend.entity.ChatMessage;
import devPilot.backend.entity.MessageRole;
import devPilot.backend.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Generation step: call OpenAI via Spring AI and stream tokens to the browser over SSE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatStreamHandler {

    private final ChatModel chatModel;
    private final ChatMessageRepository chatMessageRepository;
    private final CitationMapper citationMapper;

    public SseEmitter stream(
            UUID sessionId,
            ChatMessageResponse savedUserMessage,
            List<CitationDto> citations,
            String systemPrompt,
            String userPrompt) {

        SseEmitter emitter = new SseEmitter(RagSettings.STREAM_TIMEOUT_MS);
        StringBuilder fullReply = new StringBuilder();

        try {
            emitter.send(SseEmitter.event()
                    .name("user_message")
                    .data(savedUserMessage));

            ChatClient.builder(chatModel)
                    .build()
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .stream()
                    .content()
                    .doOnNext(token -> appendToken(emitter, fullReply, token))
                    .doOnError(err -> {
                        log.warn("Chat stream API error: {}. Delivering local fallback response.", err.getMessage());
                        String fallback = buildLocalFallback(citations, err.getMessage());
                        appendToken(emitter, fullReply, fallback);
                        completeStream(emitter, sessionId, fullReply, citations);
                    })
                    .doOnComplete(() -> completeStream(
                            emitter, sessionId, fullReply, citations))
                    .subscribe();
        } catch (Exception ex) {
            log.warn("Chat initiation error: {}. Delivering local fallback response.", ex.getMessage());
            String fallback = buildLocalFallback(citations, ex.getMessage());
            appendToken(emitter, fullReply, fallback);
            completeStream(emitter, sessionId, fullReply, citations);
        }

        return emitter;
    }

    private String buildLocalFallback(List<CitationDto> citations, String reason) {
        StringBuilder sb = new StringBuilder();
        if (reason != null && (reason.contains("429") || reason.contains("quota") || reason.contains("credit"))) {
            sb.append("⚠️ **AI Model Notice:** The external AI provider API quota has been reached (429).\n\n");
        } else {
            sb.append("⚠️ **AI Model Notice:** External AI provider returned an issue: ").append(reason).append("\n\n");
        }

        sb.append("### 📂 Relevant Codebase Context Retrieved:\n\n");
        if (citations != null && !citations.isEmpty()) {
            sb.append("Based on your query, DevPilot's RAG system successfully retrieved the following matching files from your repository:\n\n");
            for (CitationDto c : citations) {
                sb.append("- 📄 **`").append(c.filePath()).append("`** (Lines ").append(c.startLine()).append("–").append(c.endLine()).append(")\n");
            }
        } else {
            sb.append("No specific code snippets matched this question in the indexed vector store.\n");
        }

        sb.append("\n---\n💡 **Tip:** To get full natural language AI answers, you can switch to a free AI model (such as **Google Gemini** or **Groq**) in `application.properties`.\n");
        return sb.toString();
    }

    private void appendToken(SseEmitter emitter, StringBuilder fullReply, String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        fullReply.append(token);
        try {
            emitter.send(SseEmitter.event()
                    .name("token")
                    .data(token, MediaType.TEXT_PLAIN));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void completeStream(
            SseEmitter emitter,
            UUID sessionId,
            StringBuilder fullReply,
            List<CitationDto> citations) {
        try {
            ChatMessage assistant = chatMessageRepository.save(ChatMessage.builder()
                    .sessionId(sessionId)
                    .role(MessageRole.ASSISTANT)
                    .content(fullReply.toString())
                    .citations(citationMapper.toJson(citations))
                    .build());

            emitter.send(SseEmitter.event()
                    .name("assistant_message")
                    .data(toMessageResponse(assistant)));
            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                citationMapper.fromJson(message.getCitations()),
                message.getCreatedAt());
    }
}