package ru.halcyon.lingua.contracts.events;

import java.time.Instant;

/**
 * Публикует learning-core, когда в колоду добавлено новое слово.
 * Потребители (например, ai-orchestrator) могут сгенерировать примеры и упражнения.
 */
public record WordAddedEvent(
        long cardId,
        String userId,
        String word,
        String translation,
        String language,
        Instant occurredAt
) {
}
