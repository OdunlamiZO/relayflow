package io.odunlamizo.relayflow.sse;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class WorkspaceSseService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceSseService.class);

    private static final long EMITTER_TIMEOUT_MS = 5 * 60 * 1_000L;

    private static final long HEARTBEAT_INTERVAL_MS = 15 * 1_000L;

    private static final long RECONNECT_DELAY_MS = 1_000L;

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID workspaceId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        emitters.computeIfAbsent(workspaceId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable remove = () -> removeEmitter(workspaceId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(
                () -> {
                    remove.run();
                    emitter.complete();
                });
        emitter.onError(ex -> remove.run());

        try {
            emitter.send(SseEmitter.event().reconnectTime(RECONNECT_DELAY_MS).comment(""));
        } catch (IOException exception) {
            remove.run();
        }

        log.debug("SSE client subscribed {}", kv("workspaceId", workspaceId));

        return emitter;
    }

    /** Swallows failures: Spring 6.1 re-throws listener errors through the webhook's commit. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSseBroadcastEvent(SseBroadcastEvent event) {
        try {
            broadcast(event.workspaceId(), event.eventType().getEventName(), event.payload());
        } catch (Exception exception) {
            log.debug("SSE broadcast failed after commit {}", kv("error", exception.getMessage()));
        }
    }

    void broadcast(UUID workspaceId, String eventName, Object data) {
        send(workspaceId, SseEmitter.event().name(eventName).data(data));
    }

    /** Keeps idle streams from being closed by proxy timeouts, such as Next.js's 30s default. */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    void sendHeartbeats() {
        emitters.keySet().forEach(workspaceId -> send(workspaceId, SseEmitter.event().comment("")));
    }

    private void send(UUID workspaceId, SseEmitter.SseEventBuilder event) {
        List<SseEmitter> list = emitters.get(workspaceId);

        if (list == null || list.isEmpty()) {
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();

        for (SseEmitter emitter : list) {
            try {
                emitter.send(event);
            } catch (Exception exception) {
                dead.add(emitter);
            }
        }

        list.removeAll(dead);

        if (!dead.isEmpty()) {
            log.debug(
                    "Dead SSE emitters pruned {} {}",
                    kv("workspaceId", workspaceId),
                    kv("count", dead.size()));
        }
    }

    private void removeEmitter(UUID workspaceId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(workspaceId);

        if (list != null) {
            list.remove(emitter);
        }
    }
}
