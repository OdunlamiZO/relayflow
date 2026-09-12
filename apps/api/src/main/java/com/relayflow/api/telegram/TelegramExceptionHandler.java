package com.relayflow.api.telegram;

import com.relayflow.api.common.dto.ErrorResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TelegramExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(TelegramExceptionHandler.class);

    @ExceptionHandler(TelegramSendException.class)
    ResponseEntity<ErrorResponse> telegramSendFailed(TelegramSendException exception) {
        log.warn("Telegram delivery failed: {}", exception.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }
}
