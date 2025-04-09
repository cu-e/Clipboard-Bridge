package io.github.cue.clipboardbridge.server.domain.service;

import io.github.cue.clipboardbridge.server.domain.model.CommandMessage;
import io.github.cue.clipboardbridge.server.domain.model.ReplyMessage;
import io.github.cue.clipboardbridge.server.domain.port.MessageProcessor;
import io.github.cue.clipboardbridge.server.domain.port.NotificationService;
import io.github.cue.clipboardbridge.server.infrastructure.adapter.TelegramNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.security.Principal;
import java.util.List;

/**
 * Доменный сервис для обработки команд.
 * Реализует паттерн Strategy для выбора стратегии обработки команды.
 */
@Service
@Slf4j
public class CommandProcessorService implements MessageProcessor {
    
    private final NotificationService notificationService;
    
    /**
     * Конструктор с внедрением зависимостей.
     * 
     * @param notificationService сервис уведомлений
     */
    @Autowired
    public CommandProcessorService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReplyMessage processCommand(CommandMessage command, Principal principal) {
        if (command == null) {
            log.warn("Получена пустая команда");
            return ReplyMessage.builder()
                    .response("Ошибка: пустая команда")
                    .build();
        }
        
        // Получаем ID клиента из Principal или из опций сообщения
        String clientId = "unknown";
        
        if (principal != null) {
            clientId = principal.getName();
        } else {
            // Пытаемся извлечь clientId из опций сообщения
            String option = command.getOption();
            if (option != null && option.contains("client=")) {
                String[] parts = option.split("\\|");
                for (String part : parts) {
                    if (part.startsWith("client=")) {
                        clientId = part.substring(7);
                        break;
                    }
                }
            }
        }
        
        log.info("Обработка команды от {}: {}", clientId, command);
        
        // Получаем ID целевого пользователя из команды
        Long targetUserId = command.getTargetUserId();

        // Проверяем, что ID был передан
        if (targetUserId == null) {
            log.warn("Не указан ID целевого пользователя Telegram (targetUserId) в сообщении от {}", clientId);
            return ReplyMessage.builder()
                    .response("Ошибка: Не указан ID целевого пользователя Telegram в команде.")
                    .build();
        }
        
        // Выбор стратегии обработки на основе типа команды
        switch (command.getCommand()) {
            case "dm":
                // Direct Message - отправляем уведомление через Telegram с возможностью ответа
                return processDmCommand(command, clientId);
            case "broadcast":
                // Broadcast - отправляем всем
                return processBroadcastCommand(command);
            default:
                log.warn("Неизвестная команда от {}: {}", clientId, command.getCommand());
                return ReplyMessage.builder()
                        .response("Ошибка: неизвестная команда " + command.getCommand())
                        .build();
        }
    }
    
    /**
     * Обрабатывает команду прямого сообщения.
     * 
     * @param command команда для обработки
     * @param clientId идентификатор WebSocket клиента
     * @return ответ на команду
     */
    private ReplyMessage processDmCommand(CommandMessage command, String clientId) {
        String content = command.getContent();
        if (content == null || content.trim().isEmpty()) {
            return ReplyMessage.builder()
                    .response("Ошибка: пустое содержимое сообщения")
                    .build();
        }
        
        // Получаем ID целевого пользователя из команды
        Long targetUserId = command.getTargetUserId();

        // Проверяем, что ID был передан
        if (targetUserId == null) {
            log.warn("Не указан ID целевого пользователя Telegram (targetUserId) в сообщении от {}", clientId);
            return ReplyMessage.builder()
                    .response("Ошибка: Не указан ID целевого пользователя Telegram в команде.")
                    .build();
        }
        
        // Проверяем, является ли notificationService экземпляром TelegramNotificationService
        if (notificationService instanceof TelegramNotificationService) {
            TelegramNotificationService telegramService = (TelegramNotificationService) notificationService;
            
            // Отправляем уведомление указанному пользователю Telegram
            telegramService.notifyAboutClientMessage(targetUserId, clientId, content);

            return ReplyMessage.builder()
                    .response("Сообщение отправлено указанному пользователю Telegram")
                    .build();
        } else {
            // Если это не TelegramNotificationService, используем обычную отправку
            int successCount = notificationService.broadcastNotification(content);
            
            if (successCount > 0) {
                return ReplyMessage.builder()
                        .response("Сообщение успешно отправлено " + successCount + " получателям")
                        .build();
            } else {
                return ReplyMessage.builder()
                        .response("Не удалось отправить сообщение ни одному получателю")
                        .build();
            }
        }
    }
    
    /**
     * Обрабатывает команду широковещательного сообщения.
     * 
     * @param command команда для обработки
     * @return ответ на команду
     */
    private ReplyMessage processBroadcastCommand(CommandMessage command) {
        String content = command.getContent();
        if (content == null || content.trim().isEmpty()) {
            return ReplyMessage.builder()
                    .response("Ошибка: пустое содержимое широковещательного сообщения")
                    .build();
        }
        
        // Отправляем всем пользователям
        int successCount = broadcastMessage(content);
        
        return ReplyMessage.builder()
                .response("Широковещательное сообщение отправлено " + successCount + " получателям")
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int broadcastMessage(String message) {
        return notificationService.broadcastNotification(message);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public ReplyMessage processCommand(CommandMessage command) {
        // Вызов перегруженного метода без principal
        return processCommand(command, null);
    }
} 