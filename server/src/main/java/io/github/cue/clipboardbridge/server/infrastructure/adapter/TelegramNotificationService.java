package io.github.cue.clipboardbridge.server.infrastructure.adapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import io.github.cue.clipboardbridge.server.domain.model.ReplyMessage;
import io.github.cue.clipboardbridge.server.domain.port.NotificationService;
import io.github.cue.clipboardbridge.server.infrastructure.port.TelegramBotApi;
import io.github.cue.clipboardbridge.server.infrastructure.port.TelegramUpdateListener;
import io.github.cue.clipboardbridge.server.infrastructure.service.ClientSessionService;
import io.github.cue.clipboardbridge.server.infrastructure.service.WebSocketSessionMessageService;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация сервиса уведомлений через Telegram.
 * Координирует взаимодействие между Telegram ботом и WebSocket.
 */
@Service
@Primary
@Slf4j
public class TelegramNotificationService implements NotificationService, TelegramUpdateListener {

    private final Map<String, String> pendingReplies = new ConcurrentHashMap<>();
    private final Map<String, String> lastClientMessages = new ConcurrentHashMap<>();
    
    private final Set<String> processedMessageIds = new CopyOnWriteArraySet<>();
    
    private final TelegramBotApi telegramBotApi;
    private final ClientSessionService sessionService;
    private final WebSocketSessionMessageService webSocketService;
    
    public TelegramNotificationService(
            @Lazy TelegramBotApi telegramBotApi,
            ClientSessionService sessionService,
            WebSocketSessionMessageService webSocketService) {
        this.telegramBotApi = telegramBotApi;
        this.sessionService = sessionService;
        this.webSocketService = webSocketService;
        this.telegramBotApi.setUpdateListener(this);
        log.info("Инициализирован TelegramNotificationService.");
    }
    
    @Override
    public void onUpdateReceived(Update update) {
        String messageId = getUpdateIdentifier(update);
        if (messageId != null) {
            if (processedMessageIds.contains(messageId)) {
                log.debug("Пропуск дублирующегося сообщения с ID: {}", messageId);
                return;
            }
            
            processedMessageIds.add(messageId);
            
            if (processedMessageIds.size() > 1000) {
                log.debug("Очистка старых идентификаторов обработанных сообщений");
                List<String> tempList = new ArrayList<>(processedMessageIds);
                tempList.sort(Comparator.naturalOrder());
                int removeCount = tempList.size() - 500;
                if (removeCount > 0) {
                    List<String> toRemove = tempList.subList(0, removeCount);
                    processedMessageIds.removeAll(toRemove);
                }
            }
        }
        
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long userId = update.getMessage().getFrom().getId();
            
            String messageText = update.getMessage().getText();
            log.debug("Получено сообщение от пользователя {}: {}", userId, messageText);
            
            String userIdStr = userId.toString();
            if (pendingReplies.containsValue(userIdStr)) {
                String clientId = null;
                for (Map.Entry<String, String> entry : pendingReplies.entrySet()) {
                    if (entry.getValue().equals(userIdStr)) {
                        clientId = entry.getKey();
                        break;
                    }
                }
                
                if (clientId != null) {
                    if ("unknown".equals(clientId)) {
                        String savedClientId = lastClientMessages.get(userIdStr);
                        if (savedClientId != null && !savedClientId.equals("unknown")) {
                            clientId = savedClientId;
                        } else {
                            telegramBotApi.sendMessage(userId, "⚠️ Невозможно отправить ответ клиенту: неизвестный получатель");
                            pendingReplies.remove(clientId);
                            return;
                        }
                    }
                    
                    if (sendReplyToClient(clientId, messageText)) {
                        telegramBotApi.sendMessage(userId, "✅ Ваш ответ отправлен клиенту!");
                    } else {
                        telegramBotApi.sendMessage(userId, "❌ Не удалось отправить ответ клиенту. Попробуйте позже.");
                    }
                    
                    pendingReplies.remove(clientId);
                }
            } else if (messageText.startsWith("/reply")) {
                String clientId = lastClientMessages.get(userIdStr);
                if (clientId != null) {
                    if ("unknown".equals(clientId)) {
                        telegramBotApi.sendMessage(userId, "⚠️ Невозможно ответить: неизвестный получатель");
                        return;
                    }
                    
                    if (sessionService.isSessionDisconnected(clientId)) {
                        telegramBotApi.sendMessage(userId, "⚠️ Невозможно ответить: клиент отключился");
                        return;
                    }
                    
                    if (!sessionService.isSessionActive(clientId)) {
                        telegramBotApi.sendMessage(userId, "⚠️ Предупреждение: клиент может быть неактивен, ответ может не дойти");
                    }
                    
                    pendingReplies.put(clientId, userIdStr);
                    telegramBotApi.sendMessage(userId, "🔄 Введите ваш ответ для клиента:");
                } else {
                    telegramBotApi.sendMessage(userId, "⚠️ Нет сохраненных сообщений от клиентов");
                }
            } else if (messageText.startsWith("/")) {
                processCommand(userId, messageText);
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long userId = update.getCallbackQuery().getFrom().getId();
            
            if (callbackData.startsWith("reply:")) {
                String clientId = callbackData.substring(6); 
                
                if ("unknown".equals(clientId)) {
                    telegramBotApi.sendMessage(userId, "⚠️ Невозможно ответить: неизвестный получатель");
                    return;
                }
                
                if (sessionService.isSessionDisconnected(clientId)) {
                    telegramBotApi.sendMessage(userId, "⚠️ Невозможно ответить: клиент отключился");
                    return;
                }
                
                if (!sessionService.isSessionActive(clientId)) {
                    telegramBotApi.sendMessage(userId, "⚠️ Предупреждение: клиент может быть неактивен, ответ может не дойти");
                }
                
                pendingReplies.put(clientId, userId.toString());
                
                try {
                    sendNotification(userId, "🔄 Введите ваш ответ для клиента:");
                } catch (Exception e) {
                    log.error("Ошибка при обработке callback query: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Генерирует уникальный идентификатор для Update объекта, помогающий определить дубликаты.
     *
     * @param update Объект Telegram Update
     * @return Уникальный идентификатор или null, если невозможно создать
     */
    private String getUpdateIdentifier(Update update) {
        if (update.hasMessage() && update.getMessage().getMessageId() != null) {
            return "msg_" + update.getMessage().getMessageId();
        } else if (update.hasCallbackQuery() && update.getCallbackQuery().getId() != null) {
            return "cbq_" + update.getCallbackQuery().getId();
        }
        return null;
    }
    
    /**
     * Отправляет ответ клиенту через WebSocket
     * 
     * @param clientId ID клиента в WebSocket
     * @param message текст сообщения
     * @return true если сообщение успешно отправлено, false в противном случае
     */
    private boolean sendReplyToClient(String clientId, String message) {
        try {
            if (clientId == null || clientId.isEmpty() || "unknown".equals(clientId)) {
                log.error("Невозможно отправить ответ: недопустимый ID клиента: {}", clientId);
                return false;
            }
            
            ReplyMessage reply = ReplyMessage.builder()
                    .response(message)
                    .build();
            webSocketService.sendToUser(clientId, "/queue/reply", reply);
            log.info("Ответ отправлен клиенту {} через WebSocket.", clientId);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при отправке ответа клиенту {}: {}", clientId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Обрабатывает команды, полученные от пользователей.
     * 
     * @param userId ID пользователя
     * @param command команда с префиксом "/"
     */
    private void processCommand(Long userId, String command) {
        String commandLower = command.toLowerCase();
        if (commandLower.startsWith("/start")) {
            sendNotification(userId, "Приветствую! Я бот для синхронизации буфера обмена между устройствами.");
        } else if (commandLower.startsWith("/help")) {
            sendNotification(userId, "Доступные команды:\n" +
                    "/start - информация о боте\n" +
                    "/help - справка по командам\n" +
                    "/reply - ответить на последнее сообщение от клиента\n" +
                    "/stats - статистика клиентских соединений");
        } else if (commandLower.startsWith("/stats")) {
            Map<String, Object> stats = sessionService.getSessionStats();
            String statsMessage = String.format(
                    "📊 Статистика клиентских соединений:\n\n" +
                    "Активных соединений: %d\n" +
                    "Отключенных соединений: %d\n" +
                    "Всего соединений: %d\n" +
                    "Всего разрывов соединений: %d\n",
                    stats.get("activeCount"),
                    stats.get("disconnectedCount"),
                    stats.get("totalCount"),
                    stats.get("disconnectionCount")
            );
            sendNotification(userId, statsMessage);
            
            int cleanedCount = sessionService.cleanupDisconnectedSessions();
            if (cleanedCount > 0) {
                sendNotification(userId, "🧹 Очищены данные о " + cleanedCount + " отключенных сессиях");
            }
        } else {
            sendNotification(userId, "Неизвестная команда. Используйте /help для получения списка команд.");
        }
    }
    
    /**
     * Уведомляет пользователя Telegram о сообщении от клиента.
     * Добавляет кнопку "Ответить", если клиент активен.
     *
     * @param userId ID пользователя Telegram.
     * @param clientId ID клиента WebSocket.
     * @param message Текст сообщения от клиента.
     */
    public void notifyAboutClientMessage(Long userId, String clientId, String message) {
        String clientIdForLog = (clientId == null || clientId.isEmpty()) ? "unknown" : clientId;
        log.info("Подготовка уведомления для пользователя {} о сообщении от клиента {}", userId, clientIdForLog);

        lastClientMessages.put(userId.toString(), clientIdForLog);

        boolean isClientDisconnected = "unknown".equals(clientIdForLog) || sessionService.isSessionDisconnected(clientIdForLog);
        boolean isClientActive = !isClientDisconnected && sessionService.isSessionActive(clientIdForLog);

        String textToSend;
        if (isClientDisconnected) {
            Long disconnectTime = sessionService.getLastDisconnectTime(clientIdForLog);
            String disconnectTimeInfo = "";
            if (disconnectTime != null) {
                long timeSinceDisconnect = System.currentTimeMillis() - disconnectTime;
                if (timeSinceDisconnect < 60000) { disconnectTimeInfo = String.format(" %d сек. назад", timeSinceDisconnect / 1000); }
                else if (timeSinceDisconnect < 3600000) { disconnectTimeInfo = String.format(" %d мин. назад", timeSinceDisconnect / 60000); }
                else { disconnectTimeInfo = String.format(" %.1f ч. назад", timeSinceDisconnect / 3600000.0); }
            }
             textToSend = "Сообщение от клиента (ОТКЛЮЧИЛСЯ" + disconnectTimeInfo + "):\n---\n" + message + "\n\n⚠️ Клиент отключился, ответ невозможен.";
        } else if (!isClientActive) {
             textToSend = "Сообщение от клиента (НЕАКТИВЕН):\n---\n" + message + "\n\n⚠️ Клиент неактивен, ответ может не дойти.";
        } else {
             textToSend = "Сообщение от клиента (" + clientIdForLog + "):\n---\n" + message;
        }

        InlineKeyboardMarkup replyMarkup = null;
        if (isClientActive) {
            List<InlineKeyboardButton> buttonRow = new ArrayList<>();
            InlineKeyboardButton replyButton = InlineKeyboardButton.builder()
                    .text("Ответить")
                    .callbackData("reply:" + clientIdForLog)
                    .build();
            buttonRow.add(replyButton);
            replyMarkup = InlineKeyboardMarkup.builder().keyboardRow(buttonRow).build();
        }

        boolean sent;
        if (replyMarkup != null) {
            sent = telegramBotApi.sendMessageWithMarkup(userId, textToSend, replyMarkup);
        } else {
            sent = telegramBotApi.sendMessage(userId, textToSend);
        }

        if (!sent) {
            log.error("Не удалось отправить уведомление пользователю {} о сообщении от клиента {} через адаптер.", userId, clientIdForLog);
        } else {
            log.info("Уведомление для пользователя {} о сообщении от клиента {} отправлено.", userId, clientIdForLog);
        }
    }
    
    /**
     * Отправляет уведомление пользователю в Telegram.
     *
     * @param userId ID пользователя Telegram
     * @param message текст сообщения
     * @return true если сообщение успешно отправлено, false в противном случае
     */
    public boolean sendNotification(Long userId, String message) {
        // Просто делегируем вызов адаптеру
        log.debug("Отправка уведомления пользователю {} через адаптер...", userId);
        boolean sent = telegramBotApi.sendMessage(userId, message);
        if (sent) {
            log.info("Уведомление успешно отправлено пользователю {}", userId);
        } else {
             log.warn("Не удалось отправить уведомление пользователю {} через адаптер", userId);
        }
        return sent;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int broadcastNotification(String message) {
        List<Long> users = telegramBotApi.getBotUsers();
        int successCount = 0;
        for (Long userId : users) {
            if (telegramBotApi.sendMessage(userId, message)) {
                successCount++;
            }
        }
        
        log.info("Широковещательное уведомление отправлено {}/{} пользователям.", successCount, users.size());
        return successCount;
    }
    
    /**
     * Возвращает список всех пользователей бота.
     * 
     * @return неизменяемый список идентификаторов пользователей
     */
    public List<Long> getBotUsers() {
        return new ArrayList<>(telegramBotApi.getBotUsers());
    }
} 