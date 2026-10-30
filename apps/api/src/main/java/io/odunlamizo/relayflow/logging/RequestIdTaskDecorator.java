package io.odunlamizo.relayflow.logging;

// Docs: docs/codebase-components.md#requestidtaskdecorator

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

public class RequestIdTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        String requestId = MDC.get(RequestIdFilter.REQUEST_ID_KEY);

        if (requestId == null) {
            return runnable;
        }

        return () -> {
            MDC.put(RequestIdFilter.REQUEST_ID_KEY, requestId);

            try {
                runnable.run();
            } finally {
                MDC.remove(RequestIdFilter.REQUEST_ID_KEY);
            }
        };
    }
}
