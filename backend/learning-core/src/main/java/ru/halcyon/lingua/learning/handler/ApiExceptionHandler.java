package ru.halcyon.lingua.learning.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.halcyon.lingua.learning.exceptions.WordAlreadyExistsException;
import ru.halcyon.lingua.learning.exceptions.WordMisspelledException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WordAlreadyExistsException.class)
    public ProblemDetail wordAlreadyExists(WordAlreadyExistsException e) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setProperty("code", "WORD_ALREADY_EXISTS");
        return problem;
    }

    @ExceptionHandler(WordMisspelledException.class)
    public ProblemDetail wordMisspelled(WordMisspelledException e) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
        problem.setProperty("code", "WORD_MISSPELLED");
        problem.setProperty("suggestions", e.suggestions());
        return problem;
    }
}
