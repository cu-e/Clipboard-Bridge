package io.github.cue.clipboardbridge.client.domain.service;

import io.github.cue.clipboardbridge.client.domain.model.ReplyMessage;

/**
 * Интерфейс для прослушивания ответов от сервера.
 */
public interface ServerReplyListener {

    /**
     * Вызывается при получении ответа от сервера.
     *
     * @param reply Ответное сообщение от сервера.
     */
    void onReplyReceived(ReplyMessage reply);

    /**
     * Вызывается при возникновении ошибки во время получения или обработки ответа.
     *
     * @param throwable Ошибка.
     */
    void onError(Throwable throwable);
} 