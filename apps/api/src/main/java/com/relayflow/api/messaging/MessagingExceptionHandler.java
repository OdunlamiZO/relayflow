package com.relayflow.api.messaging;

import com.relayflow.api.common.dto.ErrorResponse;
import java.time.Instant;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MessagingExceptionHandler {

    @ExceptionHandler(ConversationLockedException.class)
    ResponseEntity<ErrorResponse> conversationLocked(ConversationLockedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }
}
