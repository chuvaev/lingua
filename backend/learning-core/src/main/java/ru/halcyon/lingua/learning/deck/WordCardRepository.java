package ru.halcyon.lingua.learning.deck;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;

public interface WordCardRepository extends ReactiveCrudRepository<WordCard, Long> {

    Flux<WordCard> findByUserIdOrderByCreatedAtDesc(String userId);
}
