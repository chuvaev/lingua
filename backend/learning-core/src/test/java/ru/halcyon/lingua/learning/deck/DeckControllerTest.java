package ru.halcyon.lingua.learning.deck;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

/**
 * Быстрый тест без поднятия Spring-контекста и инфраструктуры.
 * Интеграционные тесты с Testcontainers появятся отдельно.
 */
class DeckControllerTest {

    private WordCardRepository cards;
    private WordEventsPublisher events;
    private WebTestClient client;
    private SpellChecker spellChecker;

    @BeforeEach
    void setUp() {
        cards = mock(WordCardRepository.class);
        events = mock(WordEventsPublisher.class);
        spellChecker = mock(SpellChecker.class);
        client = WebTestClient.bindToController(new DeckController(cards, events, spellChecker)).build();
    }

    @Test
    void addCardSavesAndPublishesEvent() {
        var saved = new WordCard(1L, "ilya", "apple", "яблоко", "en", Instant.now());
        when(cards.save(any())).thenReturn(Mono.just(saved));
        when(events.wordAdded(saved)).thenReturn(Mono.empty());

        client.post().uri("/api/learning/cards")
                .header("X-User-Id", "ilya")
                .bodyValue(new DeckController.AddCardRequest("apple", "яблоко", "en", true))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.word").isEqualTo("apple");

        verify(events).wordAdded(saved);
    }
}
