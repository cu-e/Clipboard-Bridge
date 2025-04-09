package io.github.cue.clipboardbridge.server.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Основной класс серверного приложения.
 * Инициализирует Spring-контекст и запускает приложение.
 */
@SpringBootApplication
@ComponentScan("io.github.cue.clipboardbridge.server")
public class ServerApplication {

    /**
     * Точка входа в серверное приложение.
     * 
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }
} 