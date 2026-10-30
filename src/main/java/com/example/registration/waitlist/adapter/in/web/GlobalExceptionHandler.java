package com.example.registration.waitlist.adapter.in.web;

import com.example.registration.waitlist.domain.Identifiers;
import com.example.registration.waitlist.domain.LearnerNotFoundException;
import com.example.registration.waitlist.domain.OfferingNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Maps invalid input and unknown references to RFC 7807 {@link ProblemDetail} bodies with an {@code outcome}
 * property. Response bodies never include stack traces or raw request input.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String OUTCOME_PROPERTY = "outcome";

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INVALID_DETAIL =
            "learnerId and offeringId are required and must match " + Identifiers.PATTERN_TEXT;

    @ExceptionHandler(LearnerNotFoundException.class)
    ProblemDetail handleLearnerNotFound(LearnerNotFoundException ex) {
        LOGGER.debug("Rejected request for unknown learner={}", ex.learnerId());
        return problem(HttpStatus.NOT_FOUND, "Learner not found", ex.getMessage(), Outcome.LEARNER_NOT_FOUND);
    }

    @ExceptionHandler(OfferingNotFoundException.class)
    ProblemDetail handleOfferingNotFound(OfferingNotFoundException ex) {
        LOGGER.debug("Rejected request for unknown offering={}", ex.offeringId());
        return problem(HttpStatus.NOT_FOUND, "Offering not found", ex.getMessage(), Outcome.OFFERING_NOT_FOUND);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        HttpMessageNotReadableException.class
    })
    ProblemDetail handleInvalidRequest(Exception ex) {
        LOGGER.debug("Rejected invalid request: {}", ex.getClass().getSimpleName());
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", INVALID_DETAIL, Outcome.INVALID_REQUEST);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail, Outcome outcome) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty(OUTCOME_PROPERTY, outcome);
        return problem;
    }
}
