package io.github.cue.clipboardbridge.client.domain.service;

import io.github.cue.clipboardbridge.client.domain.model.CommandMessage;
import io.github.cue.clipboardbridge.client.domain.model.ReplyMessage;

/**
 * Интерфейс для работы с сообщениями.
 * Определяет операции для отправки и получения сообщений.
 * Следует принципу инверсии зависимостей (DIP).
 */
public interface MessageService {
    
    /**
     * Подключается к серверу сообщений.
     * 
     * @return true если подключение успешно установлено
     */
    boolean connect();
    
    /**
     * Отправляет сообщение на сервер.
     * 
     * @param command объект сообщения-команды
     * @return true, если сообщение было успешно отправлено (не гарантирует доставку или ответ)
     */
    boolean sendMessage(CommandMessage command);
    
    /**
     * Закрывает соединение с сервером.
     */
    void disconnect();
} 