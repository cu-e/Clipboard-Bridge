package io.github.cue.clipboardbridge.client.infrastructure.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Конфигурация для работы с WebSocket и STOMP.
 * Реализует паттерн Factory для создания клиентов и конвертеров.
 */
@Configuration
public class MessagingConfig {
    
    /**
     * Создает SockJS клиент для WebSocket соединений.
     * 
     * @return сконфигурированный SockJS клиент
     */
    @Bean
    public WebSocketClient webSocketClient() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        return new SockJsClient(transports);
    }
    
    /**
     * Создает STOMP клиент для работы по WebSocket.
     * 
     * @param sockJsClient сконфигурированный SockJS клиент
     * @return сконфигурированный STOMP клиент
     */
    @Bean
    public WebSocketStompClient stompClient(WebSocketClient webSocketClient) {
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(messageConverter());
        return stompClient;
    }
    
    /**
     * Создает конвертер сообщений для сериализации/десериализации JSON.
     * 
     * @return сконфигурированный конвертер
     */
    @Bean
    public MappingJackson2MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper());
        return converter;
    }
} 