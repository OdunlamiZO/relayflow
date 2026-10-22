package io.odunlamizo.relayflow.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void reusesValidIncomingRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "abc-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdDuringChain = new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (chainRequest, chainResponse) ->
                        requestIdDuringChain.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY)));

        assertThat(requestIdDuringChain.get()).isEqualTo("abc-123");
        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_KEY)).isNull();
    }

    @Test
    void generatesRequestIdWhenIncomingValueIsUnsafe() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "bad\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (chainRequest, chainResponse) -> {});

        String requestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);

        assertThat(requestId).isNotEqualTo("bad\nvalue");
        assertThat(UUID.fromString(requestId)).isNotNull();
    }
}
