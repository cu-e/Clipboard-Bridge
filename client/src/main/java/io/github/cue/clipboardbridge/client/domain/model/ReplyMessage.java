package io.github.cue.clipboardbridge.client.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Доменная модель сообщения-ответа.
 * Представляет ответ, полученный от сервера.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyMessage {
    private String response;
} 