package io.github.cue.clipboardbridge.server.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Основной класс бота для мост-сервиса буфера обмена.
 * Отвечает за обработку входящих сообщений Telegram и управление подключенными пользователями.
 * 
 * DEPRECATED: Использование этого бота устарело, вместо него используется TelegramNotificationService.
 * Этот класс помечен как устаревший, так как его функциональность дублируется с TelegramNotificationService.
 * В будущих версиях он будет удалён.
 */
//@Component - отключено для предотвращения конфликта с TelegramNotificationService
@Slf4j
public class ClipboardBridgeBot extends TelegramLongPollingBot {

    private final String botUsername;
    private final CopyOnWriteArrayList<Long> authorizedUsers = new CopyOnWriteArrayList<>();
    
    public ClipboardBridgeBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.main.user.id}") Long mainUserId) {
        super(botToken);
        this.botUsername = botUsername;
        
        authorizedUsers.add(mainUserId);
        log.info("ClipboardBridgeBot успешно инициализирован: @{}", botUsername);
    }
    
    @Override
    public String getBotUsername() {
        return botUsername;
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long userId = update.getMessage().getFrom().getId();
            String messageText = update.getMessage().getText();
            
            log.debug("Получено сообщение от пользователя {}: {}", userId, messageText);
            
            if (messageText.startsWith("/")) {
                processCommand(userId, messageText);
            }
        }
    }
    
    /**
     * Обрабатывает команды, полученные от пользователей.
     * 
     * @param userId ID пользователя
     * @param command команда с префиксом "/"
     */
    private void processCommand(Long userId, String command) {
        switch (command.toLowerCase()) {
            case "/start":
                sendMessage(userId, "Приветствую! Я бот для синхронизации буфера обмена между устройствами.");
                if (!authorizedUsers.contains(userId)) {
                    sendMessage(userId, "Вы не авторизованы для использования этого бота.");
                }
                break;
            case "/help":
                sendMessage(userId, "Доступные команды:\n/start - информация о боте\n/help - справка по командам");
                break;
            default:
                sendMessage(userId, "Неизвестная команда. Используйте /help для получения списка команд.");
        }
    }
    
    /**
     * Отправляет сообщение указанному пользователю.
     * 
     * @param userId ID пользователя
     * @param text текст сообщения
     */
    public boolean sendMessage(Long userId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(userId.toString());
            message.setText(text);
            execute(message);
            return true;
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения пользователю {}: {}", userId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Проверяет, авторизован ли пользователь.
     * 
     * @param userId ID пользователя
     * @return true если пользователь авторизован
     */
    public boolean isUserAuthorized(Long userId) {
        return authorizedUsers.contains(userId);
    }
} 