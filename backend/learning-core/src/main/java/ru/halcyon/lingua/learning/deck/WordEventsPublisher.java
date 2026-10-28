package ru.halcyon.lingua.learning.deck;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import ru.halcyon.lingua.contracts.Topics;
import ru.halcyon.lingua.contracts.events.WordAddedEvent;
import tools.jackson.databind.ObjectMapper;

/**
 * Публикация событий в Kafka без Reactor Kafka:
 * KafkaTemplate возвращает CompletableFuture, который оборачивается в Mono.
 * Сериализуем сами в JSON-строку — так формат сообщения под полным контролем.
 */
@Component
public class WordEventsPublisher {

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;

    public WordEventsPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka;
        this.json = json;
    }

    public Mono<Void> wordAdded(WordCard card) {
        var event = new WordAddedEvent(
                card.id(), card.userId(), card.word(), card.translation(), card.language(), card.createdAt());
        return Mono.fromCallable(() -> json.writeValueAsString(event))
                .flatMap(payload -> Mono.fromFuture(() -> kafka.send(Topics.WORD_ADDED, card.userId(), payload)))
                .then();
    }
}
