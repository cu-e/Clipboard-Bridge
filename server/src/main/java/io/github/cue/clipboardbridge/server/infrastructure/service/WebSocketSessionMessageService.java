package io.github.cue.clipboardbridge.server.infrastructure.service;

import io.github.cue.clipboardbridge.server.domain.model.ReplyMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Сервис для обработки сообщений WebSocket.
 * Обрабатывает отправку сообщений с проверкой активности сессии.
 */
@Service
@Slf4j
public class WebSocketSessionMessageService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ClientSessionService sessionService;

    @Autowired
    public WebSocketSessionMessageService(SimpMessagingTemplate messagingTemplate, 
                                         ClientSessionService sessionService) {
        this.messagingTemplate = messagingTemplate;
        this.sessionService = sessionService;
    }

    /**
     * Отправляет сообщение клиенту через WebSocket с проверкой состояния сессии.
     * 
     * @param clientId ID клиента
     * @param destination адрес назначения
     * @param payload полезная нагрузка сообщения
     * @return true если сообщение успешно отправлено, false в противном случае
     */
    public boolean sendToUser(String clientId, String destination, Object payload) {
        try {
            if (clientId == null || clientId.isEmpty() || "unknown".equals(clientId)) {
                log.error("Невозможно отправить сообщение: недопустимый ID клиента: {}", clientId);
                return false;
            }
            
            if (!sessionService.isSessionActive(clientId)) {
                if (sessionService.isSessionDisconnected(clientId)) {
                    log.warn("Невозможно отправить сообщение: клиент отключился: {}", clientId);
                    return false;
                } else {
                    log.warn("Невозможно отправить сообщение: клиент неактивен: {}", clientId);
                }
            }
            
            messagingTemplate.convertAndSendToUser(
                    clientId,
                    destination,
                    payload
            );
            
            log.debug("Отправлено сообщение клиенту {} на {}: {}", clientId, destination, payload);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при отправке сообщения клиенту {}: {}", clientId, e.getMessage(), e);
            
            if (isConnectionResetError(e)) {
                log.warn("Обнаружен обрыв соединения для клиента: {}", clientId);
                if (!sessionService.isSessionDisconnected(clientId)) {
                    sessionService.disconnectSession(clientId);
                }
            }
            
            return false;
        }
    }
    
    /**
     * Проверяет, является ли исключение ошибкой обрыва соединения.
     * 
     * @param e исключение
     * @return true если это ошибка обрыва соединения
     */
    private boolean isConnectionResetError(Throwable e) {
        if (e == null) {
            return false;
        }
        
        if (e.getMessage() != null) {
            String errorMessage = e.getMessage().toLowerCase();
            return errorMessage.contains("connection reset") || 
                   errorMessage.contains("no session") ||
                   errorMessage.contains("not connected") ||
                   errorMessage.contains("broken pipe") ||
                   errorMessage.contains("connection closed");
        }
        
        if (e.getCause() != null && e.getCause() != e) {
            return isConnectionResetError(e.getCause());
        }
        
        return false;
    }
    
    /**
     * Отправляет ответное сообщение клиенту через WebSocket.
     * 
     * @param clientId ID клиента
     * @param responseText текст ответа
     * @return true если сообщение успешно отправлено, false в противном случае
     */
    public boolean sendReplyToClient(String clientId, String responseText) {
        ReplyMessage reply = ReplyMessage.builder()
                .response(responseText)
                .build();
        
        return sendToUser(clientId, "/queue/reply", reply);
    }

    /**
     * Отправляет широковещательное сообщение всем подписчикам указанного адреса.
     * 
     * @param destination адрес назначения (например, "/topic/messages")
     * @param payload полезная нагрузка сообщения
     */
    public void broadcast(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("Отправлено широковещательное сообщение на {}: {}", destination, payload);
        } catch (Exception e) {
            log.error("Ошибка при отправке широковещательного сообщения на {}: {}", destination, e.getMessage(), e);
        }
    }
} 