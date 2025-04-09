package io.github.cue.clipboardbridge.client.infrastructure.service;

import io.github.cue.clipboardbridge.client.domain.model.CommandMessage;
import io.github.cue.clipboardbridge.client.domain.service.MessageService;
import io.github.cue.clipboardbridge.client.domain.service.ServerReplyListener;
import io.github.cue.clipboardbridge.client.infrastructure.config.ServerConfig;
import io.github.cue.clipboardbridge.client.infrastructure.messaging.WebSocketStompHandler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Реализация сервиса для работы с сообщениями через WebSocket и STOMP протокол.
 * Следует паттерну Facade, скрывая сложность взаимодействия с WebSocket за простым интерфейсом.
 */
@Service
@Slf4j
public class WebSocketMessageService implements MessageService {

    private final WebSocketStompClient stompClient;
    private final ServerConfig serverConfig;
    private final ServerReplyListener serverReplyListener;
    private final int timeoutSeconds;
    
    private StompSession session;
    
    /**
     * Создает сервис сообщений с указанными параметрами.
     * 
     * @param stompClient STOMP клиент
     * @param serverConfig конфигурация сервера
     * @param serverReplyListener слушатель ответов сервера
     * @param timeoutSeconds таймаут операций в секундах
     */
    @Autowired
    public WebSocketMessageService(
            WebSocketStompClient stompClient,
            ServerConfig serverConfig,
            @Lazy ServerReplyListener serverReplyListener,
            @Value("${messaging.timeout.seconds:30}") int timeoutSeconds) {
        this.stompClient = stompClient;
        this.serverConfig = serverConfig;
        this.serverReplyListener = serverReplyListener;
        this.timeoutSeconds = timeoutSeconds;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean connect() {
        if (session != null && session.isConnected()) {
            log.debug("Уже подключено к серверу сообщений.");
            return true;
        }

        try {
            WebSocketStompHandler sessionHandler = new WebSocketStompHandler(serverReplyListener);
            
            String serverUrl = serverConfig.getServerUrl();
            log.info("Подключение к серверу сообщений: {}", serverUrl);
            session = stompClient.connectAsync(serverUrl, sessionHandler).get(timeoutSeconds, TimeUnit.SECONDS);
            return session.isConnected();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Ошибка при подключении к серверу сообщений: {}", e.getMessage(), e);
            if (serverReplyListener != null) {
                serverReplyListener.onError(e);
            }
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendMessage(CommandMessage command) {
        if (session == null || !session.isConnected()) {
            if (!connect()) {
                log.error("Невозможно отправить сообщение: нет подключения к серверу");
                return false;
            }
        }
        
        try {
            log.info("Отправка сообщения: {}", command);
            session.send("/app/sendMessage", command);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
            if (serverReplyListener != null) {
                serverReplyListener.onError(e);
            }
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("Соединение с сервером сообщений разорвано");
        }
    }
} 