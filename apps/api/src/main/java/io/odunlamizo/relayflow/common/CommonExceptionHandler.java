package io.odunlamizo.relayflow.common;

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.common.dto.ErrorResponse;
import java.time.Instant;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.server.ResponseStatusException;

/** Lowest precedence so more specific advice beans handle their exceptions first. */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommonExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException exception) {
        log.debug("Resource not found {}", kv("reason", exception.getMessage()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException exception) {
        log.info(
                "Request rejected {} {}",
                kv("statusCode", HttpStatus.CONFLICT.value()),
                kv("reason", exception.getMessage()));

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        String message =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(
                                fieldError ->
                                        fieldError.getField()
                                                + ": "
                                                + fieldError.getDefaultMessage())
                        .collect(Collectors.joining("; "));

        log.info(
                "Request rejected {} {}",
                kv("statusCode", HttpStatus.BAD_REQUEST.value()),
                kv("reason", message));

        return ResponseEntity.badRequest()
                .body(
                        new ErrorResponse(
                                message.isBlank() ? "Request validation failed" : message,
                                Instant.now()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(
            MissingServletRequestParameterException exception) {
        log.info(
                "Request rejected {} {}",
                kv("statusCode", HttpStatus.BAD_REQUEST.value()),
                kv("reason", exception.getMessage()));

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException exception) {
        log.warn(
                "Request rejected {} {}",
                kv("statusCode", HttpStatus.FORBIDDEN.value()),
                kv("reason", exception.getMessage()));

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException exception) {
        log.info(
                "Request rejected {} {}",
                kv("statusCode", exception.getStatusCode().value()),
                kv("reason", exception.getReason()));

        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErrorResponse(exception.getReason(), Instant.now()));
    }

    /** SSE responses are committed as text/event-stream, so a timeout gets no body. */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    ResponseEntity<Void> asyncTimeout() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        log.error("Unhandled exception", exception);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An unexpected error occurred", Instant.now()));
    }
}
