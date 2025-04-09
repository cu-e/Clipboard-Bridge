package io.github.cue.clipboardbridge.server.infrastructure.port;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Интерфейс для обработки обновлений от Telegram бота.
 */
public interface TelegramUpdateListener {
    void onUpdateReceived(Update update);
} 