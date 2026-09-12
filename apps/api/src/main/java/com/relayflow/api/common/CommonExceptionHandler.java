package com.relayflow.api.common;

import com.relayflow.api.common.dto.ErrorResponse;
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

/**
 * {@code @Order(LOWEST_PRECEDENCE)} — without it, Spring picks among tied-priority
 * {@code @RestControllerAdvice} beans in an unspecified order, and this class's catch-all {@code
 * Exception} handler can intercept an exception before a more specific advice bean (e.g. {@link
 * com.relayflow.api.workflow.WorkflowExceptionHandler}) ever gets a chance.
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommonExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException exception) {
        log.debug("Resource not found: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ErrorResponse> illegalArgument(IllegalArgumentException exception) {
        log.warn("Illegal argument: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        log.warn("Request validation failed: {}", exception.getMessage());

        String message =
                exception.getBindingResult().getFieldErrors().stream()
                        .map(
                                fieldError ->
                                        fieldError.getField()
                                                + ": "
                                                + fieldError.getDefaultMessage())
                        .collect(Collectors.joining("; "));

        return ResponseEntity.badRequest()
                .body(
                        new ErrorResponse(
                                message.isBlank() ? "Request validation failed" : message,
                                Instant.now()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(
            MissingServletRequestParameterException exception) {
        log.warn("Missing request parameter: {}", exception.getMessage());

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    /**
     * Without this, {@link AccessDeniedException}s (e.g. {@code SseController}'s workspace
     * membership check) fall through to {@link #unexpected} and surface as a generic 500 — and for
     * SSE requests, the JSON body can't even be written since the client's {@code Accept:
     * text/event-stream} header doesn't match it.
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException exception) {
        log.warn("Request rejected: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    /**
     * Preserves the status and reason of {@link ResponseStatusException}s thrown by services (e.g.
     * authentication failures) — without this, they would fall through to {@link #unexpected} and
     * surface as a generic 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException exception) {
        log.warn("Request rejected: {}", exception.getReason());

        return ResponseEntity.status(exception.getStatusCode())
                .body(new ErrorResponse(exception.getReason(), Instant.now()));
    }

    /**
     * SSE emitters time out after {@code WorkspaceSseService.EMITTER_TIMEOUT_MS} by design — the
     * client reconnects automatically. Returning a bodiless response avoids {@code
     * HttpMessageNotWritableException} from writing a JSON {@link ErrorResponse} onto a response
     * already committed as {@code text/event-stream}.
     */
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
