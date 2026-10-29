package io.odunlamizo.relayflow.publicapi;

import static net.logstash.logback.argument.StructuredArguments.kv;

import io.odunlamizo.relayflow.authentication.ApiKeyAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Answers a {@code /public/v1} request without a valid API key with a 401 problem. */
@Component
public class PublicApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log =
            LoggerFactory.getLogger(PublicApiAuthenticationEntryPoint.class);

    private final PublicApiProblemWriter problemWriter;

    public PublicApiAuthenticationEntryPoint(PublicApiProblemWriter problemWriter) {
        this.problemWriter = problemWriter;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException)
            throws IOException {
        boolean keyMissing =
                !StringUtils.hasText(request.getHeader(ApiKeyAuthenticationFilter.HEADER));
        PublicApiProblem problem =
                keyMissing ? PublicApiProblem.MISSING_API_KEY : PublicApiProblem.INVALID_API_KEY;
        String detail =
                keyMissing
                        ? "Send your API key in the "
                                + ApiKeyAuthenticationFilter.HEADER
                                + " header."
                        : "The API key is unknown, revoked, or expired.";

        log.info(
                "Request rejected {} {}",
                kv("statusCode", problem.status().value()),
                kv("code", problem.name()));

        problemWriter.write(request, response, problem, detail);
    }
}
