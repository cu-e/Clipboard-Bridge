package io.github.cue.clipboardbridge.server.infrastructure.config;

import java.security.Principal;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import lombok.extern.slf4j.Slf4j;

/**
 * Перехватчик сообщений для сохранения Principal между SockJS и STOMP сообщениями.
 * Обеспечивает корректную передачу информации о пользователе в контроллеры.
 */
@Slf4j
public class PrincipalPreservingChannelInterceptor implements ChannelInterceptor {

    /**
     * Вызывается перед отправкой сообщения в канал.
     * Сохраняет и логирует Principal пользователя.
     *
     * @param message сообщение
     * @param channel канал
     * @return модифицированное сообщение или исходное, если не требуется модификация
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            Principal principal = accessor.getUser();
            
            if (principal != null) {
                String clientId = principal.getName();
                log.debug("Channel Interceptor - Principal: {}", clientId);
            } else {
                log.debug("Channel Interceptor - Principal отсутствует");
                
                Object sessionPrincipal = accessor.getSessionAttributes() != null ? 
                        accessor.getSessionAttributes().get("PRINCIPAL") : null;
                
                if (sessionPrincipal instanceof Principal) {
                    log.debug("Восстановление Principal из атрибутов сессии: {}", 
                            ((Principal) sessionPrincipal).getName());
                    accessor.setUser((Principal) sessionPrincipal);
                }
            }
        }
        
        return message;
    }
} 