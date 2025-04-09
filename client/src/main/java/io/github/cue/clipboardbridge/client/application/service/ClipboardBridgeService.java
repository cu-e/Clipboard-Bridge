package io.github.cue.clipboardbridge.client.application.service;

import io.github.cue.clipboardbridge.client.domain.model.CommandMessage;
import io.github.cue.clipboardbridge.client.domain.model.ReplyMessage;
import io.github.cue.clipboardbridge.client.domain.service.ClipboardService;
import io.github.cue.clipboardbridge.client.domain.service.MessageService;
import io.github.cue.clipboardbridge.client.infrastructure.config.ClientUserConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Сервис приложения, координирующий работу других сервисов.
 * Реализует паттерн Facade для упрощения взаимодействия с доменными сервисами.
 */
@Service
@Slf4j
public class ClipboardBridgeService {

    private final ClipboardService clipboardService;
    private final MessageService messageService;
    private final ClientUserConfig clientUserConfig;
    
    /**
     * Создает сервис приложения с указанными зависимостями.
     * 
     * @param clipboardService сервис буфера обмена
     * @param messageService сервис сообщений
     * @param clientUserConfig конфигурация пользователя клиента
     */
    @Autowired
    public ClipboardBridgeService(ClipboardService clipboardService, MessageService messageService, ClientUserConfig clientUserConfig) {
        this.clipboardService = clipboardService;
        this.messageService = messageService;
        this.clientUserConfig = clientUserConfig;
    }
    
    /**
     * Отправляет текст из буфера обмена на сервер.
     * 
     * @return сообщение об успехе или ошибке
     */
    public String sendClipboardContent() {
        if (!clipboardService.isClipboardAvailable()) {
            return "Буфер обмена недоступен";
        }
        
        String clipboardText = clipboardService.getFromClipboard();
        if (clipboardText == null || clipboardText.trim().isEmpty()) {
            return "Буфер обмена пуст";
        }
        
        boolean sent = sendMessage(clipboardText);
        if (sent) {
            return "Сообщение отправлено на сервер...";
        } else {
            return "Ошибка при отправке содержимого буфера обмена.";
        }
    }
    
    /**
     * Отправляет указанный текст на сервер, используя ID пользователя из конфигурации.
     *
     * @param text Текст для отправки.
     * @return true, если сообщение было успешно отправлено (инициировано).
     */
    public boolean sendMessage(String text) {
        Long targetUserId = clientUserConfig.getTargetUserId();
        
        if (targetUserId == null) {
            log.error("ID целевого пользователя Telegram не установлен. Используйте аргумент -p <user_id> для установки.");
            return false;
        }
        
        CommandMessage command = CommandMessage.builder()
                .command("dm")
                .option("-m")
                .content(text)
                .targetUserId(targetUserId)
                .build();
        
        if (!messageService.connect()) {
            log.error("Не удалось подключиться к серверу для отправки сообщения.");
            return false;
        }
        
        boolean sent = messageService.sendMessage(command);
        
        if (sent) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Принимает текст от сервера и помещает его в буфер обмена.
     * 
     * @param text текст для помещения в буфер обмена
     * @return true если операция выполнена успешно
     */
    public boolean receiveMessage(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Получено пустое сообщение");
            return false;
        }
        
        if (!clipboardService.isClipboardAvailable()) {
            log.error("Буфер обмена недоступен для записи полученного сообщения");
            return false;
        }
        
        return clipboardService.copyToClipboard(text);
    }

    /**
     * Разрывает соединение с сервером сообщений.
     */
    public void disconnect() {
        if (messageService != null) {
            messageService.disconnect();
        }
    }

    /**
     * Получает содержимое буфера обмена.
     * Возвращает null, если буфер недоступен или пуст.
     */
    public String getClipboardContent() {
        if (!clipboardService.isClipboardAvailable()) {
            log.warn("Буфер обмена недоступен при попытке чтения.");
            return null;
        }
        String clipboardText = clipboardService.getFromClipboard();
        if (clipboardText == null || clipboardText.trim().isEmpty()) {
            log.warn("Буфер обмена пуст при попытке чтения.");
            return null;
        }
        return clipboardText;
    }

    /**
     * Возвращает статус ошибки, если буфер обмена был недоступен или пуст.
     */
    public String getClipboardErrorStatus() {
        if (!clipboardService.isClipboardAvailable()) {
            return "Ошибка: Буфер обмена недоступен";
        }
        return "Ошибка: Буфер обмена пуст";
    }
} 