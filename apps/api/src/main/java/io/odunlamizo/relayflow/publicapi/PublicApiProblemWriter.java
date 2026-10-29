package io.odunlamizo.relayflow.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.odunlamizo.relayflow.publicapi.dto.ProblemResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Writes {@code application/problem+json} directly to the response, so filters that reject a
 * request before it reaches a controller answer in the same format as the controller does.
 */
@Component
public class PublicApiProblemWriter {

    public static final String PATH_PREFIX = "/public/v1";

    private final ObjectMapper objectMapper;

    public PublicApiProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static boolean isPublicApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());

        return path.equals(PATH_PREFIX) || path.startsWith(PATH_PREFIX + "/");
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            PublicApiProblem problem,
            String detail)
            throws IOException {
        response.setStatus(problem.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        objectMapper.writeValue(
                response.getOutputStream(),
                new ProblemResponse(
                        problem.type(),
                        problem.title(),
                        problem.status().value(),
                        detail,
                        request.getRequestURI(),
                        problem.name()));
    }
}
