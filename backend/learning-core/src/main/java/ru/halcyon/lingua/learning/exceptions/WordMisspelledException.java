package ru.halcyon.lingua.learning.exceptions;

import java.util.List;

public class WordMisspelledException extends RuntimeException{
    private final List<String> suggestions;

    public WordMisspelledException(String word, List<String> suggestions) {
        super("Возможно, в слове «" + word + "» опечатка");
        this.suggestions = suggestions;
    }

    public List<String> suggestions() { return suggestions; }
}
