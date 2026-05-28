package com.relayflow.api.messaging;

import com.relayflow.api.messaging.dto.ErrorResponse;
import com.relayflow.api.telegram.TelegramSendException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MessagingExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MessagingExceptionHandler.class);

    @ExceptionHandler(TelegramSendException.class)
    ResponseEntity<ErrorResponse> telegramSendFailed(TelegramSendException exception) {
        log.warn("Telegram delivery failed: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        log.warn("Request validation failed: {}", exception.getMessage());

        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Request validation failed", Instant.now()));
    }
}
