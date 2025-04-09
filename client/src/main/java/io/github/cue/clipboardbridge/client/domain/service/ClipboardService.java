package io.github.cue.clipboardbridge.client.domain.service;

/**
 * Интерфейс для работы с буфером обмена.
 * Соответствует принципу инверсии зависимостей (DIP).
 */
public interface ClipboardService {
    
    /**
     * Проверяет доступность буфера обмена.
     * 
     * @return true если буфер обмена доступен
     */
    boolean isClipboardAvailable();
    
    /**
     * Копирует текст в системный буфер обмена.
     * 
     * @param text текст для копирования
     * @return true если операция выполнена успешно
     */
    boolean copyToClipboard(String text);
    
    /**
     * Получает текст из системного буфера обмена.
     * 
     * @return текст из буфера обмена или null в случае ошибки
     */
    String getFromClipboard();
} 