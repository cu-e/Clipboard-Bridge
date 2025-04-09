package io.github.cue.clipboardbridge.server.domain.port;

/**
 * Порт для отправки уведомлений.
 * Определяет интерфейс для работы с внешними системами уведомлений.
 */
public interface NotificationService {
    
    /**
     * Отправляет уведомление пользователю.
     * 
     * @param userId идентификатор пользователя
     * @param message текст уведомления
     * @return true, если уведомление успешно отправлено
     */
    boolean sendNotification(Long userId, String message);
    
    /**
     * Отправляет уведомление всем пользователям.
     * 
     * @param message текст уведомления
     * @return количество пользователей, которым успешно отправлено уведомление
     */
    int broadcastNotification(String message);
} 