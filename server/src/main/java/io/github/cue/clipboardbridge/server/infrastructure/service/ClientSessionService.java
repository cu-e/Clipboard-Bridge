package io.github.cue.clipboardbridge.server.infrastructure.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Сервис для управления и отслеживания клиентских сессий.
 * Сохраняет информацию о активных WebSocket соединениях.
 */
@Service
@Slf4j
public class ClientSessionService {
    
    private final Map<String, Long> activeSessions = new ConcurrentHashMap<>();
    
    private final Set<String> disconnectedSessions = new CopyOnWriteArraySet<>();
    
    private int totalSessionCount = 0;
    private int disconnectionCount = 0;
    
    private final Map<String, Long> lastDisconnectTime = new ConcurrentHashMap<>();
    
    /**
     * Регистрирует новую клиентскую сессию.
     * 
     * @param clientId ID клиента
     */
    public synchronized void registerSession(String clientId) {
        activeSessions.put(clientId, System.currentTimeMillis());
        disconnectedSessions.remove(clientId);
        totalSessionCount++;
        log.info("Зарегистрирована новая клиентская сессия: {}", clientId);
    }
    
    /**
     * Обновляет время последней активности сессии.
     * 
     * @param clientId ID клиента
     */
    public void updateSession(String clientId) {
        activeSessions.put(clientId, System.currentTimeMillis());
        disconnectedSessions.remove(clientId);
    }
    
    /**
     * Отмечает сессию как отключившуюся.
     * 
     * @param clientId ID клиента
     * @return true если сессия была активна и теперь отключена, false если она уже была отключена
     */
    public synchronized boolean disconnectSession(String clientId) {
        if (clientId == null) {
            log.warn("Попытка отключить сессию с null clientId");
            return false;
        }
        
        if (disconnectedSessions.contains(clientId)) {
            log.debug("Клиентская сессия уже отмечена как отключенная: {}", clientId);
            return false;
        }
        
        Long activeTime = activeSessions.get(clientId);
        
        activeSessions.remove(clientId);
        boolean added = disconnectedSessions.add(clientId);
        
        if (!added) {
            log.warn("Обнаружена гонка данных при отключении сессии: {}", clientId);
            return false;
        }
        
        disconnectionCount++;
        
        long currentTime = System.currentTimeMillis();
        lastDisconnectTime.put(clientId, currentTime);
        
        if (activeTime != null) {
            long sessionDuration = currentTime - activeTime;
            log.info("Клиентская сессия отключена: {} (продолжительность: {} мс)", clientId, sessionDuration);
        } else {
            log.info("Клиентская сессия отключена: {}", clientId);
        }
        
        return true;
    }
    
    /**
     * Проверяет, активна ли сессия.
     * 
     * @param clientId ID клиента
     * @return true если сессия активна, false в противном случае
     */
    public boolean isSessionActive(String clientId) {
        return activeSessions.containsKey(clientId) && !disconnectedSessions.contains(clientId);
    }
    
    /**
     * Проверяет, была ли сессия отключена.
     * 
     * @param clientId ID клиента
     * @return true если сессия была отключена, false в противном случае
     */
    public boolean isSessionDisconnected(String clientId) {
        return disconnectedSessions.contains(clientId);
    }
    
    /**
     * Получает время последней активности сессии.
     * 
     * @param clientId ID клиента
     * @return время последней активности в миллисекундах или null, если сессия не найдена
     */
    public Long getLastActivityTime(String clientId) {
        return activeSessions.get(clientId);
    }
    
    /**
     * Удаляет информацию о сессии.
     * 
     * @param clientId ID клиента
     */
    public void removeSession(String clientId) {
        activeSessions.remove(clientId);
        disconnectedSessions.remove(clientId);
        log.info("Информация о клиентской сессии удалена: {}", clientId);
    }
    
    /**
     * Получает количество активных сессий.
     * 
     * @return количество активных сессий
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * Получает количество отключенных сессий.
     * 
     * @return количество отключенных сессий
     */
    public int getDisconnectedSessionCount() {
        return disconnectedSessions.size();
    }
    
    /**
     * Получает общую статистику сессий.
     * 
     * @return карта со статистикой
     */
    public Map<String, Object> getSessionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeCount", getActiveSessionCount());
        stats.put("disconnectedCount", getDisconnectedSessionCount());
        stats.put("totalCount", totalSessionCount);
        stats.put("disconnectionCount", disconnectionCount);
        return stats;
    }
    
    /**
     * Очищает неактивные сессии для экономии памяти.
     * Удаляет все отключенные сессии.
     * 
     * @return количество удаленных сессий
     */
    public int cleanupDisconnectedSessions() {
        int count = disconnectedSessions.size();
        disconnectedSessions.clear();
        log.info("Очищены данные о {} отключенных сессиях", count);
        return count;
    }
    
    /**
     * Получает время последнего отключения сессии.
     * 
     * @param clientId ID клиента
     * @return время последнего отключения в миллисекундах или null, если информация не найдена
     */
    public Long getLastDisconnectTime(String clientId) {
        return lastDisconnectTime.get(clientId);
    }
    
    /**
     * Получает продолжительность последней активной сессии клиента.
     * 
     * @param clientId ID клиента
     * @return продолжительность сессии в миллисекундах или null, если информация не доступна
     */
    public Long getLastSessionDuration(String clientId) {
        Long disconnectTime = lastDisconnectTime.get(clientId);
        Long connectTime = activeSessions.get(clientId);
        
        if (disconnectTime != null && connectTime != null) {
            return disconnectTime - connectTime;
        }
        
        return null;
    }
} 