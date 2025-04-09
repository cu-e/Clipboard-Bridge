package io.github.cue.clipboardbridge.server.infrastructure.port;

import java.util.List;

/**
 * Интерфейс для взаимодействия с Telegram Bot API.
 */
public interface TelegramBotApi {

    /**
     * Отправляет текстовое сообщение указанному пользователю.
     *
     * @param userId ID пользователя.
     * @param text Текст сообщения.
     * @return true, если отправка прошла успешно (на уровне API).
     */
    boolean sendMessage(Long userId, String text);

    /**
     * Отправляет текстовое сообщение с инлайн-клавиатурой указанному пользователю.
     *
     * @param userId ID пользователя.
     * @param text Текст сообщения.
     * @param markup Инлайн-клавиатура.
     * @return true, если отправка прошла успешно (на уровне API).
     */
    boolean sendMessageWithMarkup(Long userId, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup);

    /**
     * Регистрирует слушателя для получения обновлений от бота.
     *
     * @param listener Слушатель обновлений.
     */
    void setUpdateListener(TelegramUpdateListener listener);

    /**
     * Возвращает список ID пользователей бота.
     *
     * @return Список ID.
     */
    List<Long> getBotUsers();
    
    /**
     * Возвращает имя пользователя бота.
     *
     * @return Имя пользователя бота.
     */
    String getBotUsername();

} 