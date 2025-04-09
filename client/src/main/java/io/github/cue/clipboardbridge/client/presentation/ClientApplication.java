package io.github.cue.clipboardbridge.client.presentation;

import io.github.cue.clipboardbridge.client.application.service.ClipboardBridgeService;
import io.github.cue.clipboardbridge.client.domain.model.ReplyMessage;
import io.github.cue.clipboardbridge.client.domain.service.ServerReplyListener;
import io.github.cue.clipboardbridge.client.infrastructure.config.ClientUserConfig;
import io.github.cue.clipboardbridge.client.infrastructure.config.ServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Основной класс клиентского приложения.
 * Реализует интерфейс CommandLineRunner для обработки аргументов командной строки.
 */
@SpringBootApplication
@ComponentScan("io.github.cue.clipboardbridge.client")
@Slf4j
public class ClientApplication implements CommandLineRunner, ServerReplyListener {

    private final ClipboardBridgeService clipboardBridgeService;
    private final CommandLineProcessor commandLineProcessor;
    private final ClientUserConfig clientUserConfig;

    // Future для ожидания завершения обработки (успешного копирования или ошибки)
    private final CompletableFuture<Void> processingCompleteFuture = new CompletableFuture<>();

    /**
     * Конструктор с внедрением зависимостей через Spring DI.
     * 
     * @param clipboardBridgeService сервис, координирующий работу приложения
     * @param commandLineProcessor обработчик командной строки
     * @param clientUserConfig конфигурация пользователя клиента (для получения ID)
     */
    @Autowired
    public ClientApplication(ClipboardBridgeService clipboardBridgeService, CommandLineProcessor commandLineProcessor, ClientUserConfig clientUserConfig) {
        this.clipboardBridgeService = clipboardBridgeService;
        this.commandLineProcessor = commandLineProcessor;
        this.clientUserConfig = clientUserConfig;
    }

    /**
     * Точка входа в приложение.
     * 
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        
        SpringApplication application = new SpringApplication(ClientApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }

    /**
     * Метод, выполняемый после инициализации Spring-контекста.
     * Обрабатывает аргументы командной строки и выполняет соответствующие действия.
     * 
     * @param args аргументы командной строки
     */
    @Override
    public void run(String... args) {
        log.info("Запуск клиентского приложения...");
        String result = "Неизвестная ошибка."; // Инициализация по умолчанию
        int exitCode = 1;

        try {
            // 1. Парсинг аргументов
            commandLineProcessor.parseArguments(args);

            // 2. Получение ID (если был флаг -p)
            Long targetUserId = clientUserConfig.getTargetUserId();
            if (targetUserId == null) {
                result = "Ошибка: Не указан ID целевого пользователя Telegram. Используйте аргумент -p <user_id>.";
                log.error(result);
                System.err.println(result);
                return; 
            }

            // 3. Текста сообщения
            String messageToSend;
            boolean useClipboard = false;
            if (commandLineProcessor.hasMessageOverride()) {
                messageToSend = commandLineProcessor.getMessageText();
                log.info("Используется указанное сообщение: {}", messageToSend);
            } else {
                log.info("Используется содержимое буфера обмена");
                useClipboard = true;
                messageToSend = clipboardBridgeService.getClipboardContent(); 
                if (messageToSend == null) {
                    result = clipboardBridgeService.getClipboardErrorStatus();
                    log.warn(result);
                    System.err.println(result);
                    return; 
                }
            }

            // 4. Отправка сообщения
            boolean messageSent;
            if (useClipboard) {

                result = clipboardBridgeService.sendClipboardContent();
                messageSent = result.startsWith("Сообщение отправлено");
            } else {
                messageSent = clipboardBridgeService.sendMessage(messageToSend);
                result = messageSent ? "Сообщение отправлено на сервер..." : "Ошибка: Не удалось отправить сообщение на сервер.";
            }

            if (messageSent) {
                log.info(result);
                System.out.println(result);

                log.info("Ожидание ответа от сервера...");

                processingCompleteFuture.get();
                log.info("Обработка завершена успешно.");
                exitCode = 0; 
            } else {
                log.error(result);
                System.err.println(result);
            }

        } catch (ExecutionException e) {
            result = "Ошибка обработки: " + e.getCause().getMessage();
            log.error("Ошибка во время ожидания ответа: {}", e.getCause().getMessage(), e.getCause());
            System.err.println(result);
        } catch (InterruptedException e) {
            result = "Операция прервана.";
            log.warn(result, e);
            Thread.currentThread().interrupt(); 
        } catch (Exception e) {
            result = "Произошла ошибка: " + e.getMessage();
            log.error("Произошла ошибка при выполнении операции: {}", e.getMessage(), e);
            System.err.println(result);
        } finally {
            log.debug("Завершение работы приложения с кодом {}", exitCode);
            try {
                clipboardBridgeService.disconnect();
            } catch (Exception e) {
                log.warn("Ошибка при отключении от сервера: {}", e.getMessage());
            }
            System.exit(exitCode); 
        }
    }

    /**
     * Обрабатывает ответ, полученный от сервера.
     *
     * @param reply Ответное сообщение от сервера.
     */
    @Override
    public void onReplyReceived(ReplyMessage reply) {
        log.info("Получен асинхронный ответ от сервера: {}", reply.getResponse());
        System.out.println("Ответ сервера: " + reply.getResponse());
        
        boolean copied = clipboardBridgeService.receiveMessage(reply.getResponse());
        if (copied) {
            log.info("Ответ сервера скопирован в буфер обмена.");
            System.out.println("(Скопировано в буфер обмена)");

            processingCompleteFuture.complete(null);
        } else {
            log.warn("Не удалось скопировать ответ в буфер обмена. Завершение Future с ошибкой.");
            processingCompleteFuture.completeExceptionally(new RuntimeException("Не удалось скопировать ответ в буфер обмена."));
        }
    }

    /**
     * Обрабатывает ошибку, возникшую при взаимодействии с WebSocket.
     *
     * @param throwable Ошибка.
     */
    @Override
    public void onError(Throwable throwable) {
        log.error("Ошибка WebSocket: {}", throwable.getMessage(), throwable);
        System.err.println("Ошибка WebSocket: " + throwable.getMessage());
        //TODO: добавить логику для попытки переподключения или уведомления пользователя!

        processingCompleteFuture.completeExceptionally(throwable);
    }
} 