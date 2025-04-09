package io.github.cue.clipboardbridge.server.infrastructure.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Пользовательский обработчик рукопожатия WebSocket.
 * Обеспечивает установку Principal для WebSocket сессий.
 */
@Slf4j
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    /**
     * Определяет Principal для WebSocket сессии.
     * 
     * @param request HTTP запрос
     * @param wsHandler обработчик WebSocket
     * @param attributes атрибуты сессии
     * @return Principal для сессии
     */
    @Override
    protected Principal determineUser(ServerHttpRequest request, 
                                     WebSocketHandler wsHandler, 
                                     Map<String, Object> attributes) {
        Object principalObject = attributes.get("PRINCIPAL");
        if (principalObject instanceof Principal) {
            Principal principal = (Principal) principalObject;
            log.info("Использование Principal для WebSocket: {}", principal.getName());
            return principal;
        }
        
        Object clientIdObject = attributes.get("CLIENT_ID");
        if (clientIdObject instanceof String) {
            String clientId = (String) clientIdObject;
            log.info("Создание Principal из CLIENT_ID: {}", clientId);
            return new DefaultPrincipalHandshakeInterceptor.StompPrincipal(clientId);
        }
        
        Principal requestPrincipal = request.getPrincipal();
        if (requestPrincipal != null) {
            log.info("Использование Principal из запроса: {}", requestPrincipal.getName());
            return requestPrincipal;
        }
        
        log.warn("Principal не найден для WebSocket соединения");
        return super.determineUser(request, wsHandler, attributes);
    }
} 