package ru.halcyon.lingua.ai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

/**
 * Диалог с репетитором. Ответ стримится токенами через SSE,
 * поэтому первые слова появляются у пользователя сразу, а не после всей генерации.
 */
@RestController
@RequestMapping("/api/ai")
public class ChatController {

    private static final String SYSTEM_PROMPT = """
            You are a friendly English tutor for a Russian-speaking learner.
            Answer in simple English that matches the learner's level.
            If the learner makes a mistake, show the corrected sentence first,
            then briefly explain the rule in Russian.
            The learner id is {userId}; use it when calling tools.
            """;

    private final ChatClient chat;
    private final TutorTools tools;

    public ChatController(ChatClient.Builder builder, TutorTools tools) {
        this.chat = builder.build();
        this.tools = tools;
    }

    @PostMapping(path = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestHeader("X-User-Id") String userId, @RequestBody ChatRequest request) {
        return chat.prompt()
                .system(s -> s.text(SYSTEM_PROMPT).param("userId", userId))
                .user(request.message())
                .tools(tools)
                .stream()
                .content();
    }

    public record ChatRequest(String message) {
    }
}
