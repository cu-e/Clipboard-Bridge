package io.github.cue.clipboardbridge.server.infrastructure.config;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import lombok.extern.slf4j.Slf4j;

/**
 * Перехватчик рукопожатия WebSocket для установки Principal.
 * Реализует паттерн Interceptor для добавления информации о пользователе.
 */
@Slf4j
public class DefaultPrincipalHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * Вызывается перед рукопожатием WebSocket для установки Principal.
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                  WebSocketHandler wsHandler, Map<String, Object> attributes) {
        final String sessionId = UUID.randomUUID().toString();
        log.info("Установка Principal для WebSocket соединения: {}", sessionId);
        
        attributes.put("PRINCIPAL", new StompPrincipal(sessionId));
        
        attributes.put("CLIENT_ID", sessionId);
        
        return true;
    }

    /**
     * Вызывается после завершения рукопожатия WebSocket.
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("Ошибка при установке WebSocket-соединения: {}", exception.getMessage());
        }
    }

    /**
     * Внутренний класс для представления Principal в STOMP сессии.
     */
    public static class StompPrincipal implements Principal {
        private final String name;

        public StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String toString() {
            return "StompPrincipal[" + name + "]";
        }
    }
} 