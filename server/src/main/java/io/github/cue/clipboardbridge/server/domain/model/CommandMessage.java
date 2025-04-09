package io.github.cue.clipboardbridge.server.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Доменная модель сообщения-команды.
 * Представляет команду, полученную от клиента.
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