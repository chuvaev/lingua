package ru.halcyon.lingua.learning.deck;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Карточка слова в колоде пользователя.
 * Поля интервального повторения (FSRS) добавятся отдельной миграцией.
 */
@Table("word_card")
public record WordCard(
        @Id Long id,
        String userId,
        String word,
        String translation,
        String language,
        Instant createdAt
) {

    public static WordCard create(String userId, String word, String translation, String language) {
        return new WordCard(null, userId, word, translation, language, Instant.now());
    }
}
