package io.github.cue.clipboardbridge.server.domain.port;

import io.github.cue.clipboardbridge.server.domain.model.CommandMessage;
import io.github.cue.clipboardbridge.server.domain.model.ReplyMessage;

import java.security.Principal;

/**
 * Порт для обработки сообщений.
 * Определяет интерфейс обработки сообщений в соответствии с гексагональной архитектурой.
 */
public interface MessageProcessor {
    
    /**
     * Обрабатывает входящее сообщение-команду и формирует ответ.
     * 
     * @param command входящее сообщение-команда
     * @return сформированный ответ
     */
    ReplyMessage processCommand(CommandMessage command);
    
    /**
     * Обрабатывает входящее сообщение-команду с информацией о пользователе и формирует ответ.
     * 
     * @param command входящее сообщение-команда
     * @param principal информация о пользователе
     * @return сформированный ответ
     */
    ReplyMessage processCommand(CommandMessage command, Principal principal);
    
    /**
     * Отправляет сообщение всем подключенным клиентам.
     * 
     * @param message текст сообщения для отправки
     * @return количество клиентов, которым успешно отправлено сообщение
     */
    int broadcastMessage(String message);
} 