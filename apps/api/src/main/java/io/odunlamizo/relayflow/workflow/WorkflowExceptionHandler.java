package io.odunlamizo.relayflow.workflow;

// Docs: docs/codebase-components.md#workflowexceptionhandler

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.common.dto.ErrorResponse;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WorkflowExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExceptionHandler.class);

    @ExceptionHandler(WorkflowValidationException.class)
    ResponseEntity<ErrorResponse> workflowValidation(WorkflowValidationException exception) {
        log.info("Workflow validation failed {}", kv("reason", exception.getMessage()));

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(exception.getMessage(), Instant.now()));
    }
}
