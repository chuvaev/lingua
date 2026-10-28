package ru.halcyon.lingua.content.ingest;

import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Внутренний API (через Traefik наружу не публикуется).
 * VectorStore блокирующий (JDBC + HTTP к Ollama), поэтому каждый вызов
 * уходит в boundedElastic и не занимает event loop WebFlux.
 */
@RestController
@RequestMapping("/internal/materials")
public class MaterialsController {

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public MaterialsController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<IngestResult> ingest(@RequestBody IngestRequest request) {
        return Mono.fromCallable(() -> {
                    var source = new Document(request.text(), Map.of(
                            "title", request.title(),
                            "language", request.language()));
                    List<Document> chunks = splitter.apply(List.of(source));
                    vectorStore.add(chunks);
                    return new IngestResult(request.title(), chunks.size());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/search")
    public Mono<List<SearchHit>> search(@RequestParam String q, @RequestParam(defaultValue = "5") int limit) {
        return Mono.fromCallable(() -> vectorStore.similaritySearch(
                        SearchRequest.builder().query(q).topK(limit).build()))
                .subscribeOn(Schedulers.boundedElastic())
                .map(docs -> docs.stream()
                        .map(d -> new SearchHit(d.getText(), String.valueOf(d.getMetadata().get("title")), d.getScore()))
                        .toList());
    }

    public record IngestRequest(String title, String language, String text) {
    }

    public record IngestResult(String title, int chunks) {
    }

    public record SearchHit(String text, String title, Double score) {
    }
}
