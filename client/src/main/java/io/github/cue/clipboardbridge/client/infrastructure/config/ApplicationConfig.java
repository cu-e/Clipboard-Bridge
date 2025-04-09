package io.github.cue.clipboardbridge.client.infrastructure.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Конфигурация приложения для загрузки пользовательских настроек.
 * Реализует паттерн Factory для создания и настройки компонентов.
 */
@Configuration
@Slf4j
public class ApplicationConfig {
    
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".clipboard-bridge";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "client-config.properties";
    private static final String USER_CONFIG_SOURCE = "userConfig";
    
    @Autowired
    private ConfigurableEnvironment environment;
    
    /**
     * Инициализирует конфигурацию приложения, загружая пользовательские настройки.
     * Вызывается после создания бина.
     */
    @PostConstruct
    public void init() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                Properties userProps = new Properties();
                userProps.load(in);
                
                if (userProps.containsKey("server.url")) {
                    PropertiesPropertySource propertySource = new PropertiesPropertySource(USER_CONFIG_SOURCE, userProps);
                    environment.getPropertySources().addFirst(propertySource);
                    
                    log.info("Загружены пользовательские настройки из {}: server.url={}", 
                            CONFIG_FILE, userProps.getProperty("server.url"));
                } else {
                    log.warn("В файле конфигурации отсутствует свойство server.url");
                }
            } catch (IOException e) {
                log.error("Ошибка при загрузке пользовательских настроек: {}", e.getMessage());
            }
        } else {
            log.info("Файл пользовательских настроек не найден, используются стандартные настройки");
        }
    }
    
    /**
     * Создает ServerConfig с учетом пользовательских настроек.
     * 
     * @return экземпляр ServerConfig
     */
    @Bean
    @Primary
    public ServerConfig serverConfig() {
        return new ServerConfig();
    }
} 