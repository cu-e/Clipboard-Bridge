package io.github.cue.clipboardbridge.server.infrastructure.config;

import io.github.cue.clipboardbridge.server.infrastructure.adapter.TelegramBotAdapter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Конфигурация для регистрации Telegram бота.
 * Использует паттерн Factory для создания и регистрации бота.
 */
@Configuration
@Slf4j
@Getter
public class TelegramBotConfig {

    @Value("${telegram.bot.username}")
    private String botUsername;
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Value("${telegram.main.user.id}")
    private Long mainUserId;

    private TelegramBotsApi telegramBotsApi;

    private final TelegramBotAdapter telegramBotAdapter;

    public TelegramBotConfig(TelegramBotAdapter telegramBotAdapter) {
        this.telegramBotAdapter = telegramBotAdapter;
    }

    /**
     * Регистрирует бота в Telegram API.
     * TelegramNotificationService используется в качестве единственного бота.
     * 
     * @param notificationBot Основной сервис уведомлений Telegram
     * @return API для работы с ботами Telegram
     * @throws TelegramApiException если произошла ошибка при регистрации бота
     */
    @Bean
    @Primary
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        try {
            this.telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            log.info("Telegram Bot Config: Бот @{} должен быть зарегистрирован адаптером.", telegramBotAdapter.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("Ошибка инициализации Telegram Bots API: {}", e.getMessage(), e);
            throw e;
        }
        return telegramBotsApi;
    }
} 