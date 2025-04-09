package io.github.cue.clipboardbridge.client.infrastructure.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Класс для хранения настроек сервера и управления ими.
 * Следует принципу единственной ответственности (SRP).
 */
@Component
@Slf4j
@Getter
public class ServerConfig {
    
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".clipboard-bridge";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "client-config.properties";
    private static final String SERVER_URL_PROPERTY = "server.url";
    private static final String DEFAULT_SERVER_URL = "http://localhost:8080/ws";
    
    @Value("${server.url:" + DEFAULT_SERVER_URL + "}")
    private String serverUrl;
    
    /**
     * Инициализирует конфигурацию при запуске приложения.
     * Загружает сохраненные настройки, если они существуют.
     */
    public ServerConfig() {
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
                log.info("Создана директория для конфигурации: {}", CONFIG_DIR);
            }
            
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(configFile)) {
                    props.load(in);
                    
                    String savedUrl = props.getProperty(SERVER_URL_PROPERTY);
                    if (savedUrl != null && !savedUrl.isEmpty()) {
                        log.info("Найден сохраненный URL сервера: {}", savedUrl);
                    }
                }
            } else {
                saveConfig();
                log.info("Создан файл конфигурации с URL сервера по умолчанию: {}", DEFAULT_SERVER_URL);
            }
        } catch (IOException e) {
            log.error("Ошибка при инициализации конфигурации: {}", e.getMessage());
        }
    }
    
    /**
     * Обновляет URL сервера и сохраняет его в конфигурационный файл.
     * 
     * @param newServerUrl новый URL сервера
     * @return true если обновление прошло успешно
     */
    public boolean updateServerUrl(String newServerUrl) {
        this.serverUrl = newServerUrl;
        return saveConfig();
    }
    
    /**
     * Сохраняет текущую конфигурацию в файл.
     * 
     * @return true если сохранение прошло успешно
     */
    private boolean saveConfig() {
        Properties props = new Properties();
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileInputStream in = new FileInputStream(configFile)) {
                    props.load(in);
                } catch (IOException e) {
                    log.warn("Не удалось загрузить существующий файл конфигурации: {}", e.getMessage());
                }
            }
            
            String urlToSave = (serverUrl != null) ? serverUrl : DEFAULT_SERVER_URL;
            props.setProperty(SERVER_URL_PROPERTY, urlToSave);
            
            try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
                props.store(out, "Clipboard Bridge Client Configuration");
                log.info("Конфигурация сервера сохранена: {}", urlToSave);
                return true;
            }
        } catch (IOException e) {
            log.error("Ошибка при сохранении конфигурации: {}", e.getMessage());
            return false;
        }
    }
} 