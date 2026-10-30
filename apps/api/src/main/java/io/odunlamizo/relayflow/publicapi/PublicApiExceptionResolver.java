package io.odunlamizo.relayflow.publicapi;

// Docs: docs/codebase-components.md#publicapiexceptionresolver

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.common.ResourceNotFoundException;
import io.odunlamizo.relayflow.contact.ContactNotFoundException;
import io.odunlamizo.relayflow.contact.InvalidContactTagValueException;
import io.odunlamizo.relayflow.contact.UnknownContactTagException;
import io.odunlamizo.relayflow.messaging.ConversationLockedException;
import io.odunlamizo.relayflow.messaging.ConversationNotFoundException;
import io.odunlamizo.relayflow.telegram.TelegramSendException;
import io.odunlamizo.relayflow.whatsapp.WhatsAppSendException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Answers every {@code /public/v1} error with problem details. A resolver rather than a controller
 * advice, so it also covers errors raised before a controller is chosen, such as an unknown path or
 * method. Runs ahead of the {@code @ExceptionHandler} advice beans, which keep the internal API's
 * error format.
 */
@Component
public class PublicApiExceptionResolver implements HandlerExceptionResolver, Ordered {

    private static final Logger log = LoggerFactory.getLogger(PublicApiExceptionResolver.class);

    private final PublicApiProblemWriter problemWriter;

    public PublicApiExceptionResolver(PublicApiProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public ModelAndView resolveException(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            Object handler,
            @NonNull Exception exception) {
        if (!PublicApiProblemWriter.isPublicApiRequest(request) || response.isCommitted()) {
            return null;
        }

        Resolution resolution = resolve(exception, request);
        logResolution(resolution, exception);

        if (exception instanceof HttpRequestMethodNotSupportedException methodException
                && methodException.getSupportedHttpMethods() != null) {
            response.setHeader(
                    HttpHeaders.ALLOW,
                    StringUtils.collectionToCommaDelimitedString(
                            methodException.getSupportedHttpMethods()));
        }

        try {
            problemWriter.write(request, response, resolution.problem(), resolution.detail());
        } catch (IOException writeException) {
            log.warn("Problem response not written", writeException);
        }

        return new ModelAndView();
    }

    /** Runs after Spring Boot's error attributes resolver, which only records the exception. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private Resolution resolve(Exception exception, HttpServletRequest request) {
        return switch (exception) {
            case ConversationNotFoundException notFound ->
                    new Resolution(PublicApiProblem.CONVERSATION_NOT_FOUND, notFound.getMessage());
            case ContactNotFoundException notFound ->
                    new Resolution(PublicApiProblem.CONTACT_NOT_FOUND, notFound.getMessage());
            case ResourceNotFoundException notFound ->
                    new Resolution(PublicApiProblem.RESOURCE_NOT_FOUND, notFound.getMessage());
            case UnknownContactTagException unknownTag ->
                    new Resolution(PublicApiProblem.UNKNOWN_TAG, unknownTag.getReason());
            case InvalidContactTagValueException invalidValue ->
                    new Resolution(PublicApiProblem.INVALID_TAG_VALUE, invalidValue.getReason());
            case ConversationLockedException locked ->
                    new Resolution(PublicApiProblem.CONVERSATION_LOCKED, locked.getMessage());
            case TelegramSendException sendFailure ->
                    new Resolution(
                            PublicApiProblem.CHANNEL_DELIVERY_FAILED, sendFailure.getMessage());
            case WhatsAppSendException sendFailure ->
                    new Resolution(
                            PublicApiProblem.CHANNEL_DELIVERY_FAILED, sendFailure.getMessage());
            case AuthenticationException ignored ->
                    new Resolution(
                            PublicApiProblem.INVALID_API_KEY,
                            "The API key is unknown, revoked, or expired.");
            case MethodArgumentNotValidException invalid ->
                    new Resolution(
                            PublicApiProblem.INVALID_REQUEST,
                            invalid.getBindingResult().getFieldErrors().stream()
                                    .map(
                                            fieldError ->
                                                    fieldError.getField()
                                                            + ": "
                                                            + fieldError.getDefaultMessage())
                                    .collect(Collectors.joining("; ")));
            case HandlerMethodValidationException invalid ->
                    new Resolution(
                            PublicApiProblem.INVALID_REQUEST,
                            invalid.getAllErrors().stream()
                                    .map(MessageSourceResolvable::getDefaultMessage)
                                    .collect(Collectors.joining("; ")));
            case ConstraintViolationException invalid ->
                    new Resolution(
                            PublicApiProblem.INVALID_REQUEST,
                            invalid.getConstraintViolations().stream()
                                    .map(
                                            violation ->
                                                    violation.getPropertyPath()
                                                            + ": "
                                                            + violation.getMessage())
                                    .collect(Collectors.joining("; ")));
            case HttpMessageNotReadableException ignored ->
                    new Resolution(
                            PublicApiProblem.INVALID_REQUEST,
                            "The request body is missing or isn't valid JSON.");
            case MethodArgumentTypeMismatchException mismatch ->
                    new Resolution(
                            PublicApiProblem.INVALID_REQUEST,
                            "\""
                                    + mismatch.getValue()
                                    + "\" isn't a valid "
                                    + mismatch.getName()
                                    + ".");
            case MissingServletRequestParameterException missing ->
                    new Resolution(
                            PublicApiProblem.INVALID_REQUEST,
                            missing.getParameterName() + " is required.");
            case IllegalArgumentException invalid ->
                    new Resolution(PublicApiProblem.INVALID_REQUEST, invalid.getMessage());
            case NoResourceFoundException ignored -> endpointNotFound(request);
            case NoHandlerFoundException ignored -> endpointNotFound(request);
            case HttpRequestMethodNotSupportedException unsupported ->
                    new Resolution(
                            PublicApiProblem.METHOD_NOT_ALLOWED,
                            unsupported.getMethod() + " isn't supported for this endpoint.");
            case HttpMediaTypeNotSupportedException ignored ->
                    new Resolution(
                            PublicApiProblem.UNSUPPORTED_MEDIA_TYPE,
                            "Send the request body as application/json.");
            default ->
                    new Resolution(
                            PublicApiProblem.INTERNAL_ERROR, "An unexpected error occurred.");
        };
    }

    private Resolution endpointNotFound(HttpServletRequest request) {
        return new Resolution(
                PublicApiProblem.ENDPOINT_NOT_FOUND,
                "There's no endpoint at "
                        + request.getMethod()
                        + " "
                        + request.getRequestURI()
                        + ".");
    }

    private void logResolution(Resolution resolution, Exception exception) {
        PublicApiProblem problem = resolution.problem();

        if (problem == PublicApiProblem.INTERNAL_ERROR) {
            log.error("Unhandled exception", exception);

            return;
        }

        if (problem == PublicApiProblem.CHANNEL_DELIVERY_FAILED) {
            log.warn(
                    "Channel delivery failed {} {}",
                    kv("code", problem.name()),
                    kv("reason", resolution.detail()));

            return;
        }

        log.info(
                "Request rejected {} {} {}",
                kv("statusCode", problem.status().value()),
                kv("code", problem.name()),
                kv("reason", resolution.detail()));
    }

    private record Resolution(PublicApiProblem problem, String detail) {}
}
