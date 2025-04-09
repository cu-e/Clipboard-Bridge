package io.github.cue.clipboardbridge.server.infrastructure.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.cue.clipboardbridge.server.domain.model.CommandMessage;
import io.github.cue.clipboardbridge.server.domain.port.MessageProcessor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST-контроллер для обработки сообщений.
 * Реализует паттерн Adapter для интеграции между HTTP/WebSocket и доменными сервисами.
 */
@RestController
@RequestMapping("/api/messages")
@Slf4j
public class MessageController {

    private final MessageProcessor messageProcessor;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Конструктор с внедрением зависимостей.
     * 
     * @param messageProcessor процессор сообщений
     * @param messagingTemplate шаблон для отправки сообщений
     */
    @Autowired
    public MessageController(MessageProcessor messageProcessor, SimpMessagingTemplate messagingTemplate) {
        this.messageProcessor = messageProcessor;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Обрабатывает сообщения, полученные через REST API.
     * 
     * @param message текст сообщения
     * @return ответ сервера
     */
    @PostMapping("/send")
    public ResponseEntity<String> processMessage(@RequestBody String message) {
        try {
            log.info("Получено сообщение через REST API: {}", message);
            
            int successCount = messageProcessor.broadcastMessage(message);
            
            if (successCount > 0) {
                return ResponseEntity.ok("Сообщение успешно отправлено " + successCount + " пользователям");
            } else {
                return ResponseEntity.badRequest().body("Не удалось отправить сообщение ни одному пользователю");
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }
    
    /**
     * Обрабатывает сообщения, полученные через WebSocket/STOMP.
     * 
     * @param command команда для обработки
     * @param principal объект, представляющий пользователя
     */
    @MessageMapping("/sendMessage")
    public void processStompMessage(CommandMessage command, Principal principal) {
        String clientId = "unknown";
        
        if (principal != null) {
            clientId = principal.getName();
            log.info("Principal в сообщении: {}", principal);
        } else {
            log.warn("Principal отсутствует в STOMP сообщении");
        }
        
        log.info("Получено STOMP сообщение от {}: {}", clientId, command);
        
        try {
            if (command.getOption() == null || command.getOption().isEmpty()) {
                command.setOption("client=" + clientId);
            } else {
                command.setOption(command.getOption() + "|client=" + clientId);
            }
            
            messageProcessor.processCommand(command, principal);
            log.info("Сообщение от клиента {} обработано.", clientId);
        } catch (Exception e) {
            log.error("Ошибка при обработке STOMP сообщения от {}: {}", clientId, e.getMessage(), e);
        }
    }
} 