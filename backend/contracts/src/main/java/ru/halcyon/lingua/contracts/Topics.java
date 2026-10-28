package ru.halcyon.lingua.contracts;

/**
 * Имена топиков Kafka. Единственное место, где они объявлены.
 */
public final class Topics {

    /** Пользователь добавил слово в колоду. */
    public static final String WORD_ADDED = "lingua.learning.word-added";

    private Topics() {
    }
}
