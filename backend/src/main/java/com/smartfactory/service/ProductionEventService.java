package com.smartfactory.service;

import com.smartfactory.dto.ProductionEventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ProductionEventService {

    private static final Logger log = LoggerFactory.getLogger(ProductionEventService.class);

    private final Map<Long, List<SseEmitter>> orderEmitters = new ConcurrentHashMap<>();
    private final List<SseEmitter> globalEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribeToOrder(Long orderId) {
        SseEmitter emitter = new SseEmitter(0L);
        orderEmitters.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(orderId, emitter));
        emitter.onTimeout(() -> removeEmitter(orderId, emitter));
        emitter.onError(e -> removeEmitter(orderId, emitter));
        return emitter;
    }

    public SseEmitter subscribeGlobal() {
        SseEmitter emitter = new SseEmitter(0L);
        globalEmitters.add(emitter);
        emitter.onCompletion(() -> globalEmitters.remove(emitter));
        emitter.onTimeout(() -> globalEmitters.remove(emitter));
        emitter.onError(e -> globalEmitters.remove(emitter));
        return emitter;
    }

    public void publishEvent(ProductionEventDto event) {
        Long orderId = event.orderId();
        if (orderId != null) {
            List<SseEmitter> emitters = orderEmitters.get(orderId);
            if (emitters != null) {
                sendToEmitters(emitters, event);
            }
        }
        sendToEmitters(globalEmitters, event);
    }

    public void publishPhaseProgress(Long orderId, Object progressData) {
        ProductionEventDto event = new ProductionEventDto(
            "PHASE_PROGRESS", orderId, null, null, null,
            null, null, null, null, null,
            "Phase progress update", progressData
        );
        publishEvent(event);
    }

    private void sendToEmitters(List<SseEmitter> emitters, Object data) {
        emitters.removeIf(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("production-event")
                    .data(data));
                return false;
            } catch (IOException e) {
                log.debug("SSE emitter send failed, removing: {}", e.getMessage());
                return true;
            }
        });
    }

    private void removeEmitter(Long orderId, SseEmitter emitter) {
        List<SseEmitter> emitters = orderEmitters.get(orderId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                orderEmitters.remove(orderId);
            }
        }
    }
}
