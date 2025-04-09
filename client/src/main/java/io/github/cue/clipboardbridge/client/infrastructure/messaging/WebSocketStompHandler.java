package io.github.cue.clipboardbridge.client.infrastructure.messaging;

import io.github.cue.clipboardbridge.client.domain.model.ReplyMessage;
import io.github.cue.clipboardbridge.client.domain.service.ServerReplyListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;

/**
 * Обработчик STOMP-сессий и сообщений.
 * Реализует паттерн Adapter для работы с WebSocket STOMP соединениями.
 */
@Slf4j
public class WebSocketStompHandler extends StompSessionHandlerAdapter {

    private final ServerReplyListener listener;
    
    /**
     * Создает новый обработчик сессий с указанным future для получения ответа.
     * 
     * @param listener Слушатель для обработки входящих ответов и ошибок.
     */
    public WebSocketStompHandler(ServerReplyListener listener) {
        this.listener = listener;
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        log.info("Установлено соединение с сервером сообщений. ID сессии: {}", session.getSessionId());
        
        // Подписываемся на очередь ответов для этого пользователя
        session.subscribe("/user/queue/reply", this);
        log.info("Подписан на /user/queue/reply");
    }

    @Override
    public void handleException(StompSession session, StompCommand command, 
                               StompHeaders headers, byte[] payload, Throwable exception) {
        log.error("Ошибка при обработке сообщения STOMP: {}", exception.getMessage());
        if (listener != null) {
            listener.onError(exception);
        }
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        log.error("Ошибка транспортного уровня WebSocket: {}", exception.getMessage());
        if (listener != null) {
            listener.onError(exception);
        }
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return ReplyMessage.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        if (payload instanceof ReplyMessage) {
            ReplyMessage reply = (ReplyMessage) payload;
            log.info("Получен ответ от сервера: {}", reply);
            if (listener != null) {
                listener.onReplyReceived(reply);
            }
        } else {
            log.warn("Получен неожиданный тип данных: {}", payload.getClass().getName());
            if (listener != null) {
                listener.onError(new IllegalArgumentException("Неверный тип ответа: " + payload.getClass().getName()));
            }
        }
    }
}