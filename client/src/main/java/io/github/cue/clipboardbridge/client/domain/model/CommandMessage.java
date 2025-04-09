package io.github.cue.clipboardbridge.client.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Доменная модель сообщения-команды.
 * Представляет команду, которая отправляется на сервер.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommandMessage {
    private String command;
    private String option;
    private String content;
    private Long targetUserId;
} 