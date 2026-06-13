package com.relayflow.api.messaging;

import com.relayflow.api.messaging.dto.ErrorResponse;
import com.relayflow.api.telegram.TelegramSendException;
import com.relayflow.api.workflow.WorkflowValidationException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class MessagingExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MessagingExceptionHandler.class);

    @ExceptionHandler(TelegramSendException.class)
    ResponseEntity<ErrorResponse> telegramSendFailed(TelegramSendException exception) {
        log.warn("Telegram delivery failed: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ConversationLockedException.class)
    ResponseEntity<ErrorResponse> conversationLocked(ConversationLockedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(WorkflowValidationException.class)
    ResponseEntity<ErrorResponse> workflowValidation(WorkflowValidationException exception) {
        log.warn("Workflow validation failed: {}", exception.getMessage());

        return ResponseEntity.badRequest()
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

        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Request validation failed", Instant.now()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> missingParameter(
            MissingServletRequestParameterException exception) {
        log.warn("Missing request parameter: {}", exception.getMessage());

        return ResponseEntity.badRequest()
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
