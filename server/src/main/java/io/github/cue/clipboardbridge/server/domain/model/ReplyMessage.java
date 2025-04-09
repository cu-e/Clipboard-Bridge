package io.github.cue.clipboardbridge.server.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Доменная модель сообщения-ответа.
 * Представляет ответ, который отправляется клиенту.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplyMessage {
    private String response;
} 