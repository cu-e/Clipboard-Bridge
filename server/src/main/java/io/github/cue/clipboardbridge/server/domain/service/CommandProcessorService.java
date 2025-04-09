package io.github.cue.clipboardbridge.server.domain.service;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.cue.clipboardbridge.server.domain.model.CommandMessage;
import io.github.cue.clipboardbridge.server.domain.model.ReplyMessage;
import io.github.cue.clipboardbridge.server.domain.port.MessageProcessor;
import io.github.cue.clipboardbridge.server.domain.port.NotificationService;
import io.github.cue.clipboardbridge.server.infrastructure.adapter.TelegramNotificationService;
import lombok.extern.slf4j.Slf4j;

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
        
        String clientId = "unknown";
        
        if (principal != null) {
            clientId = principal.getName();
        } else {
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
        
        Long targetUserId = command.getTargetUserId();

        if (targetUserId == null) {
            log.warn("Не указан ID целевого пользователя Telegram (targetUserId) в сообщении от {}", clientId);
            return ReplyMessage.builder()
                    .response("Ошибка: Не указан ID целевого пользователя Telegram в команде.")
                    .build();
        }
        
        switch (command.getCommand()) {
            case "dm":
                return processDmCommand(command, clientId);
            case "broadcast":
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
        
        Long targetUserId = command.getTargetUserId();

        if (targetUserId == null) {
            log.warn("Не указан ID целевого пользователя Telegram (targetUserId) в сообщении от {}", clientId);
            return ReplyMessage.builder()
                    .response("Ошибка: Не указан ID целевого пользователя Telegram в команде.")
                    .build();
        }
        
        if (notificationService instanceof TelegramNotificationService) {
            TelegramNotificationService telegramService = (TelegramNotificationService) notificationService;
            
            telegramService.notifyAboutClientMessage(targetUserId, clientId, content);

            return ReplyMessage.builder()
                    .response("Сообщение отправлено указанному пользователю Telegram")
                    .build();
        } else {
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
     * Обрабатывает команду бродкаст сообщения.
     * 
     * @param command команда для обработки
     * @return ответ на команду
     */
    private ReplyMessage processBroadcastCommand(CommandMessage command) {
        String content = command.getContent();
        if (content == null || content.trim().isEmpty()) {
            return ReplyMessage.builder()
                    .response("Ошибка: пустое содержимое бродкаст сообщения")
                    .build();
        }
        
        int successCount = broadcastMessage(content);
        
        return ReplyMessage.builder()
                .response("Бродкаст сообщение отправлено " + successCount + " получателям")
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
        return processCommand(command, null);
    }
} 