package io.github.cue.clipboardbridge.client.presentation;

import io.github.cue.clipboardbridge.client.infrastructure.config.ClientUserConfig;
import io.github.cue.clipboardbridge.client.infrastructure.config.ServerConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Обрабатывает аргументы командной строки.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class CommandLineProcessor {

    private final ServerConfig serverConfig;
    private final ClientUserConfig clientUserConfig;

    private String messageText = null;
    private Long targetUserIdOverride = null;

    /**
     * Парсит аргументы командной строки.
     *
     * @param args Аргументы командной строки.
     */
    public void parseArguments(String... args) {
        log.debug("Парсинг аргументов командной строки...");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-ip":
                    if (i + 1 < args.length) {
                        processIpArgument(args[++i]);
                    } else {
                        log.warn("Пропущен URL сервера после аргумента -ip.");
                    }
                    break;
                case "-p":
                    if (i + 1 < args.length) {
                        processUserArgument(args[++i]);
                    } else {
                        log.warn("Пропущен ID пользователя после аргумента -p.");
                    }
                    break;
                case "-m":
                    if (i + 1 < args.length) {
                        processMessageArgument(args[++i]);
                    } else {
                        log.warn("Пропущен текст сообщения после аргумента -m.");
                    }
                    break;
                default:
                    log.warn("Неизвестный аргумент командной строки: {}", arg);
                    break;
            }
        }
        log.debug("Парсинг аргументов завершен.");
    }

    private void processIpArgument(String serverUrlArg) {
        log.info("Установка нового URL сервера из аргумента: {}", serverUrlArg);
        String newServerUrl = serverUrlArg;
        if (!newServerUrl.startsWith("http://") && !newServerUrl.startsWith("https://")) {
            newServerUrl = "http://" + newServerUrl;
        }
        if (!newServerUrl.endsWith("/ws")) {
            newServerUrl += "/ws";
        }
        serverConfig.updateServerUrl(newServerUrl);
        log.info("URL сервера обновлен: {}", serverConfig.getServerUrl());
    }

    private void processUserArgument(String userIdArg) {
        try {
            targetUserIdOverride = Long.parseLong(userIdArg);
            log.info("Используется указанный ID целевого пользователя из аргумента: {}", targetUserIdOverride);
            clientUserConfig.updateTargetUserId(targetUserIdOverride); 
        } catch (NumberFormatException e) {
            log.warn("Некорректный формат ID пользователя для -p: '{}'. Аргумент проигнорирован.", userIdArg);
        }
    }

    private void processMessageArgument(String messageArg) {
        messageText = messageArg;
        log.info("Используется указанное сообщение из аргумента: {}", messageText);
    }

    /**
     * Возвращает true, если сообщение было передано через аргумент -m.
     */
    public boolean hasMessageOverride() {
        return messageText != null;
    }
} 