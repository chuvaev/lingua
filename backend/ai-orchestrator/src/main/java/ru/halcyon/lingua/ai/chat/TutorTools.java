package ru.halcyon.lingua.ai.chat;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Инструменты, которые модель может вызывать во время диалога.
 * Сейчас это заглушки: следующий шаг — ходить в learning-core и content-rag.
 *
 * Правило для локальных моделей: инструментов немного, описания короткие и точные,
 * параметры простые. Чем больше инструментов, тем чаще модель путается.
 */
@Component
public class TutorTools {

    @Tool(description = "Get the learner's current language level and native language.")
    public LearnerProfile getLearnerProfile(@ToolParam(description = "Learner id") String userId) {
        // TODO: запрос в learning-core
        return new LearnerProfile(userId, "B1", "ru");
    }

    @Tool(description = "Save a word to the learner's vocabulary deck for spaced repetition.")
    public String addWordToDeck(
            @ToolParam(description = "Learner id") String userId,
            @ToolParam(description = "Word or phrase in the target language") String word,
            @ToolParam(description = "Translation into the learner's native language") String translation) {
        // TODO: POST /api/learning/cards в learning-core
        return "Saved: " + word + " — " + translation;
    }

    public record LearnerProfile(String userId, String level, String nativeLanguage) {
    }
}
