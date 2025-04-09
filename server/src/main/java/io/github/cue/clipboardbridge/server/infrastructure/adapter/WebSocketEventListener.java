package io.github.cue.clipboardbridge.server.infrastructure.adapter;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import io.github.cue.clipboardbridge.server.infrastructure.service.ClientSessionService;
import lombok.extern.slf4j.Slf4j;

/**
 * Компонент для отслеживания событий WebSocket/STOMP.
 * Регистрирует подключение и отключение клиентов.
 */
@Component
@Slf4j
public class WebSocketEventListener {

    private final ClientSessionService sessionService;
    
    private final Map<String, Long> processedDisconnects = new ConcurrentHashMap<>();
    
    private final Set<String> processedClientIds = new CopyOnWriteArraySet<>();
    
    private static final long DISCONNECT_EVENT_TTL = 10000; // 10 секунд

    @Autowired
    public WebSocketEventListener(ClientSessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Обрабатывает событие подключения по WebSocket.
     * 
     * @param event событие подключения
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        
        if (principal != null) {
            String clientId = principal.getName();
            log.info("Новое WebSocket соединение: {}", clientId);
            sessionService.registerSession(clientId);
            
            processedClientIds.remove(clientId);
        } else {
            log.warn("Новое соединение без Principal");
        }
    }

    /**
     * Обрабатывает событие подписки клиента.
     * Также используется для отслеживания сессий, так как у некоторых
     * клиентов Principal может появиться только при подписке.
     * 
     * @param event событие подписки
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        
        if (principal != null) {
            String clientId = principal.getName();
            log.debug("Клиент подписался на топик: {}, ID: {}", 
                    headerAccessor.getDestination(), clientId);
            sessionService.updateSession(clientId);
        }
        
        if (principal == null && headerAccessor.getSessionAttributes() != null) {
            Object sessionIdObj = headerAccessor.getSessionAttributes().get("CLIENT_ID");
            if (sessionIdObj instanceof String) {
                String clientId = (String) sessionIdObj;
                log.debug("Клиент подписался (по атрибуту CLIENT_ID): {}", clientId);
                sessionService.updateSession(clientId);
            }
        }
    }

    /**
     * Обрабатывает событие отключения по WebSocket.
     * Реализует механизм дедупликации для предотвращения повторной обработки.
     * 
     * @param event событие отключения
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        if (sessionId == null) {
            log.warn("Получено событие отключения без ID сессии");
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        cleanupExpiredEvents(currentTime);
        
        if (isAlreadyProcessed(sessionId)) {
            log.debug("Пропуск дублирующегося события отключения для сессии: {}", sessionId);
            return;
        }
        
        String clientId = extractClientId(headerAccessor);
        if (clientId == null) {
            log.warn("Соединение WebSocket закрыто для неизвестного клиента (сессия: {})", sessionId);
            return;
        }
        
        if (processedClientIds.contains(clientId)) {
            log.debug("Пропуск дублирующегося события отключения для клиента: {}", clientId);
            return;
        }
        
        log.info("WebSocket соединение закрыто: {} (сессия: {})", clientId, sessionId);
        
        markSessionAsProcessed(sessionId, currentTime);
        processedClientIds.add(clientId);
        
        if (!sessionService.isSessionDisconnected(clientId)) {
            sessionService.disconnectSession(clientId);
        }
    }
    
    /**
     * Извлекает ID клиента из заголовков сообщения.
     * 
     * @param headerAccessor заголовки STOMP сообщения
     * @return ID клиента или null, если не найден
     */
    private String extractClientId(StompHeaderAccessor headerAccessor) {
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            return principal.getName();
        }
        
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object clientIdObj = sessionAttributes.get("CLIENT_ID");
            if (clientIdObj instanceof String) {
                return (String) clientIdObj;
            }
        }
        
        return null;
    }
    
    /**
     * Проверяет, была ли сессия уже обработана.
     * 
     * @param sessionId ID сессии
     * @return true если сессия уже обработана
     */
    private synchronized boolean isAlreadyProcessed(String sessionId) {
        return processedDisconnects.containsKey(sessionId);
    }
    
    /**
     * Отмечает сессию как обработанную.
     * 
     * @param sessionId ID сессии
     * @param timestamp метка времени обработки
     */
    private synchronized void markSessionAsProcessed(String sessionId, long timestamp) {
        processedDisconnects.put(sessionId, timestamp);
    }
    
    /**
     * Очищает устаревшие записи об обработанных событиях.
     * 
     * @param currentTime текущее время
     */
    private synchronized void cleanupExpiredEvents(long currentTime) {
        processedDisconnects.entrySet().removeIf(
            entry -> (currentTime - entry.getValue()) > DISCONNECT_EVENT_TTL);
    }
} 