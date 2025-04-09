package io.github.cue.clipboardbridge.server.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Конфигурация WebSocket для обмена сообщениями.
 * Использует шаблонный метод (Template Method) через наследование от WebSocketMessageBrokerConfigurer.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Настраивает брокер сообщений.
     * 
     * @param registry реестр брокера сообщений
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        
        registry.setUserDestinationPrefix("/user");
        
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Регистрирует конечные точки STOMP.
     * 
     * @param registry реестр конечных точек
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        CustomHandshakeHandler handshakeHandler = new CustomHandshakeHandler();
        DefaultPrincipalHandshakeInterceptor interceptor = new DefaultPrincipalHandshakeInterceptor();
        
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*") 
                .setHandshakeHandler(handshakeHandler)
                .withSockJS()
                .setInterceptors(interceptor);
    }
    
    /**
     * Настраивает входящий канал сообщений для сохранения Principal
     * между соединением SockJS и получением STOMP сообщений.
     *
     * @param registration регистрация канала
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new PrincipalPreservingChannelInterceptor());
    }
} 