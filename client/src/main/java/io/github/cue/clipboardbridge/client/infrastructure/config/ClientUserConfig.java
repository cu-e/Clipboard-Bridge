package io.github.cue.clipboardbridge.client.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Component
@Slf4j
@Getter
@Setter
public class ClientUserConfig {

    private Long targetUserId;
    private final Path configFilePath;

    public ClientUserConfig(@Value("${target.user.id:#{null}}") Long targetUserId) {
        String userHome = System.getProperty("user.home");
        this.configFilePath = Paths.get(userHome, ".clipboard-bridge", "client-config.properties");
        this.targetUserId = loadTargetUserId(targetUserId); 
    }

    private Long loadTargetUserId(Long defaultValue) {
        if (Files.exists(configFilePath)) {
            try {
                Properties props = new Properties();
                props.load(Files.newInputStream(configFilePath));
                String savedId = props.getProperty("target.user.id");
                if (savedId != null && !savedId.isEmpty()) {
                    try {
                        Long parsedId = Long.parseLong(savedId);
                        log.info("Загружен сохраненный ID целевого пользователя: {}", parsedId);
                        return parsedId;
                    } catch (NumberFormatException e) {
                        log.warn("Некорректный формат сохраненного ID целевого пользователя: '{}'. Используется значение по умолчанию.", savedId);
                    }
                }
            } catch (IOException e) {
                log.warn("Ошибка при чтении файла конфигурации {}: {}. Используется значение по умолчанию.", configFilePath, e.getMessage());
            }
        }
        log.info("Используется ID целевого пользователя по умолчанию: {}", defaultValue);
        return defaultValue;
    }

    public void updateTargetUserId(Long newUserId) {
        if (newUserId == null) {
            log.warn("Попытка установить пустой ID целевого пользователя.");
            return;
        }
        if (!newUserId.equals(this.targetUserId)) {
            this.targetUserId = newUserId;
            saveTargetUserId(newUserId);
            log.info("ID целевого пользователя обновлен и сохранен: {}", newUserId);
        }
    }

    private void saveTargetUserId(Long userIdToSave) {
        try {
            Files.createDirectories(configFilePath.getParent());
            Properties props = new Properties();
            if (Files.exists(configFilePath)) {
                props.load(Files.newInputStream(configFilePath));
            }
            props.setProperty("target.user.id", String.valueOf(userIdToSave));
            props.store(Files.newOutputStream(configFilePath), "Clipboard Bridge Client Configuration");
        } catch (IOException e) {
            log.error("Не удалось сохранить ID целевого пользователя {} в файл {}: {}", userIdToSave, configFilePath, e.getMessage(), e);
        }
    }
} 