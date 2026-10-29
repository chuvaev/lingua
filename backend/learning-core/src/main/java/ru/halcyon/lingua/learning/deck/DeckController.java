package ru.halcyon.lingua.learning.deck;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.halcyon.lingua.learning.exceptions.WordAlreadyExistsException;
import ru.halcyon.lingua.learning.exceptions.WordMisspelledException;

/**
 * Пока пользователь передаётся заголовком X-User-Id.
 * Когда появится авторизация, его будет брать Spring Security из JWT.
 */
@RestController
@RequestMapping("/api/learning/cards")
public class DeckController {

    private final WordCardRepository cards;
    private final WordEventsPublisher events;
    private final SpellChecker spellChecker;

    public DeckController(WordCardRepository cards, WordEventsPublisher events, SpellChecker spellChecker) {
        this.cards = cards;
        this.events = events;
        this.spellChecker = spellChecker;
    }

    @GetMapping
    public Flux<WordCard> list(@RequestHeader("X-User-Id") String userId) {
        return cards.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<WordCard> add(@RequestHeader("X-User-Id") String userId, @RequestBody AddCardRequest request) {
        Mono<Void> check = request.force()
                ? Mono.empty()
                : spellChecker.suggestions(request.word())
                .flatMap(s -> s.isEmpty()
                        ? Mono.<Void>empty()
                        : Mono.error(new WordMisspelledException(request.word(), s)));

        return check.then(cards.save(WordCard.create(userId, request.word(), request.translation(), request.language())))
                .onErrorMap(DuplicateKeyException.class, e -> new WordAlreadyExistsException(request.word(), e))
                .flatMap(saved -> events.wordAdded(saved).thenReturn(saved));
    }

    public record AddCardRequest(String word, String translation, String language, boolean force) {
    }
}
