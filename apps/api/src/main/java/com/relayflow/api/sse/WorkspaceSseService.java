package com.relayflow.api.sse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Manages SSE connections scoped to a workspace. Callers subscribe via {@link #subscribe(UUID)} and
 * the service delivers events to all active emitters for that workspace.
 *
 * <p>Events are delivered via {@link #onSseBroadcastEvent(SseBroadcastEvent)}, which is annotated
 * with {@code @TransactionalEventListener(phase = AFTER_COMMIT)}. This guarantees the SSE push only
 * fires after the originating transaction has committed, so clients never read stale data.
 *
 * <p>Completed / timed-out emitters are removed lazily on the next broadcast.
 */
@Service
public class WorkspaceSseService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSseService.class);

    /** Emitter timeout — 5 minutes. The client must reconnect after this. */
    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1_000L;

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Creates and registers an {@link SseEmitter} for {@code workspaceId}. The emitter is removed
     * automatically on completion, timeout, or error.
     */
    public SseEmitter subscribe(UUID workspaceId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emitters.computeIfAbsent(workspaceId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> removeEmitter(workspaceId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(ex -> remove.run());

        log.debug("SSE client subscribed to workspace {}", workspaceId);

        return emitter;
    }

    /**
     * Receives {@link SseBroadcastEvent} after the originating transaction commits and pushes the
     * event to all active subscribers of the workspace.
     *
     * <p>{@code AFTER_COMMIT} ensures the new data is visible to the client's follow-up queries.
     *
     * <p>Any exception from the broadcast is caught here and not re-thrown. In Spring 6.1, {@code
     * TransactionalApplicationListenerSynchronization.processEventWithCallbacks} re-throws every
     * exception that escapes the listener, which would propagate through {@code
     * AbstractPlatformTransactionManager.processCommit} and cause the servlet to return 500 —
     * making Telegram (or any webhook caller) retry a request whose transaction already committed.
     * A dead SSE socket must never affect the HTTP response to the webhook.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSseBroadcastEvent(SseBroadcastEvent event) {
        try {
            broadcast(event.workspaceId(), event.eventType().getEventName(), event.payload());
        } catch (Exception e) {
            log.debug(
                    "SSE broadcast swallowed after commit (likely a closed socket): {}",
                    e.getMessage());
        }
    }

    /**
     * Sends {@code eventName} with {@code data} to every active emitter for {@code workspaceId}.
     * Dead emitters encountered during the broadcast are pruned in-place.
     */
    void broadcast(UUID workspaceId, String eventName, Object data) {
        List<SseEmitter> list = emitters.get(workspaceId);

        if (list == null || list.isEmpty()) {
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }

        list.removeAll(dead);

        if (!dead.isEmpty()) {
            log.debug("Pruned {} dead SSE emitter(s) for workspace {}", dead.size(), workspaceId);
        }
    }

    private void removeEmitter(UUID workspaceId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(workspaceId);

        if (list != null) {
            list.remove(emitter);
        }
    }
}
