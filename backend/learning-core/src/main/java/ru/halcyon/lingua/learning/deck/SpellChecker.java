package ru.halcyon.lingua.learning.deck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
public class SpellChecker {
    private static final Logger log = LoggerFactory.getLogger(SpellChecker.class);

    private final WebClient client;

    public SpellChecker(WebClient.Builder builder, @Value("${lingua.languagetool.url}") String url) {
        this.client = builder.baseUrl(url).build();
    }

    /** Пустой список — ошибок нет (или проверка недоступна). Иначе — варианты исправления. */
    public Mono<List<String>> suggestions(String word) {
        return client.post().uri("/v2/check")
                .body(BodyInserters.fromFormData("language", "en-US").with("text", word))
                .retrieve()
                .bodyToMono(CheckResponse.class)
                .map(r -> r.matches().stream()
                        .filter(m -> "misspelling".equals(m.rule().issueType()))
                        .flatMap(m -> m.replacements().stream().limit(3).map(Replacement::value))
                        .toList())
                .timeout(Duration.ofSeconds(2))
                // LanguageTool недоступен — не мешаем пользователю сохранить слово
                .onErrorResume(e -> {
                    log.warn("Проверка правописания недоступна: {}", e.toString());
                    return Mono.just(List.of());
                });
    }

    record CheckResponse(List<Match> matches) {}
    record Match(Rule rule, List<Replacement> replacements) {}
    record Rule(String issueType) {}
    record Replacement(String value) {}
}
