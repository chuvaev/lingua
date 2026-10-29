package ru.halcyon.lingua.learning.exceptions;

public class WordAlreadyExistsException extends RuntimeException {

    public WordAlreadyExistsException(String word, Throwable cause) {
        super("Слово «" + word + "» уже есть в словаре", cause);
    }
}
