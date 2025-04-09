package io.github.cue.clipboardbridge.server.infrastructure.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import io.github.cue.clipboardbridge.server.infrastructure.port.TelegramBotApi;
import io.github.cue.clipboardbridge.server.infrastructure.port.TelegramUpdateListener;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Адаптер для взаимодействия с Telegram Bot API.
 * Скрывает детали реализации TelegramLongPollingBot.
 */
@Component
@Slf4j
public class TelegramBotAdapter extends TelegramLongPollingBot implements TelegramBotApi {

    private final String botUsername;
    private final CopyOnWriteArrayList<Long> botUsers = new CopyOnWriteArrayList<>();
    private TelegramUpdateListener updateListener; 

    public TelegramBotAdapter(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            @Value("${telegram.main.user.id}") Long mainUserId,
            @Lazy TelegramUpdateListener listener) {
        super(botToken);
        this.botUsername = botUsername;
        this.updateListener = listener;

        if (mainUserId != null) {
            botUsers.add(mainUserId);
        }
        log.info("Инициализация Telegram Bot Adapter для @{}", botUsername);
    }

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(this);
            log.info("Telegram Bot Adapter @{} успешно зарегистрирован.", botUsername);
        } catch (TelegramApiException e) {
            log.error("Ошибка регистрации Telegram Bot Adapter @{}: {}", botUsername, e.getMessage(), e);
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (updateListener != null) {
            try {
                if (update.hasMessage() && update.getMessage().getFrom() != null) {
                    Long userId = update.getMessage().getFrom().getId();
                    if (!botUsers.contains(userId)) {
                        botUsers.addIfAbsent(userId);
                        log.info("Добавлен новый пользователь бота через адаптер: {}", userId);
                    }
                } else if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
                     Long userId = update.getCallbackQuery().getFrom().getId();
                    if (!botUsers.contains(userId)) {
                        botUsers.addIfAbsent(userId);
                        log.info("Добавлен новый пользователь бота (callback) через адаптер: {}", userId);
                    }
                }
                
                updateListener.onUpdateReceived(update);
            } catch (Exception e) {
                log.error("Ошибка при передаче обновления слушателю {}: {}", updateListener.getClass().getSimpleName(), e.getMessage(), e);
            }
        } else {
            log.warn("Получено обновление, но слушатель не зарегистрирован.");
        }
    }

    @Override
    public boolean sendMessage(Long userId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(userId.toString())
                .text(text)
                .build();
        try {
            execute(message);
            log.debug("Сообщение успешно отправлено пользователю {} через адаптер", userId);
            return true;
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения пользователю {} через адаптер: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendMessageWithMarkup(Long userId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = SendMessage.builder()
                .chatId(userId.toString())
                .text(text)
                .replyMarkup(markup)
                .build();
        try {
            execute(message);
            log.debug("Сообщение с разметкой успешно отправлено пользователю {} через адаптер", userId);
            return true;
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения с разметкой пользователю {} через адаптер: {}", userId, e.getMessage());
            return false;
        }
    }

    @Override
    public void setUpdateListener(TelegramUpdateListener listener) {
        log.info("Установлен слушатель обновлений: {}", listener.getClass().getSimpleName());
        this.updateListener = listener;
    }

    @Override
    public List<Long> getBotUsers() {
        return new ArrayList<>(botUsers);
    }
} 