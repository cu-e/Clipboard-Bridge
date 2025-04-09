# Clipboard Bridge - Клиент

Клиентская часть приложения Clipboard Bridge для обмена содержимым буфера обмена между устройствами.

### Структура проекта

```
src/main/java/io/github/cue/clipboardbridge/client/
├── application           # Слой приложения
│   └── service           # Сервисы приложения (ClipboardBridgeService)
├── domain                # Доменный слой
│   ├── model             # Доменные модели (CommandMessage, ReplyMessage)
│   └── service           # Доменные сервисы (интерфейсы: ClipboardService, MessageService, ServerReplyListener)
├── infrastructure        # Инфраструктурный слой
│   ├── config            # Конфигурации (ServerConfig, ClientUserConfig)
│   ├── messaging         # Реализация обмена сообщениями (WebSocketMessageService, WebSocketStompHandler)
│   └── service           # Реализации доменных сервисов (SystemClipboardService)
└── presentation          # Слой представления
    ├── ClientApplication # Точка входа и координатор
    └── CommandLineProcessor # Обработчик аргументов командной строки
```
## Технологии

- Java 17+
- Spring Boot
- Spring WebSocket
- STOMP (Simple Text Oriented Messaging Protocol)
- Lombok
- SLF4J & Logback
  
## Запуск приложения

### Предварительные требования

- JDK 17 или выше
- Gradle 8.0 или выше

### Сборка

```bash
./gradlew clean build
```

### Запуск

```bash
java -jar build/libs/client-0.0.1-SNAPSHOT.jar [аргументы]
```

### Аргументы командной строки

*   `-p <user_id>`: Устанавливает ID пользователя Telegram, которому будет отправлено сообщение. **Этот ID сохраняется** в файле `~/.clipboard-bridge/client-config.properties` и будет использоваться при последующих запусках, если аргумент `-p` не указан снова.
*   `-m "<текст>"`: Отправляет указанный текст вместо содержимого буфера обмена.
*   `-ip <url>`: Переопределяет URL сервера WebSocket (например, `-ip localhost:9090` или `-ip my.server.com/ws`). По умолчанию используется `http://localhost:8080/ws`.

### Примеры использования

1.  **Отправка содержимого буфера обмена (требуется предварительная установка ID пользователя):**
```bash
java -jar build/libs/client-0.0.1-SNAPSHOT.jar
```

2.  **Установка ID пользователя и отправка буфера обмена:**
```bash
java -jar build/libs/client-0.0.1-SNAPSHOT.jar -p 123456789 
```

3.  **Отправка произвольного текста указанному пользователю:**
```bash
java -jar build/libs/client-0.0.1-SNAPSHOT.jar -p 123456789 -m "Текст для отправки"
```

4.  **Отправка буфера обмена на другой сервер:**
```bash
java -jar build/libs/client-0.0.1-SNAPSHOT.jar -ip 192.168.1.100:8080 
```

## Разработка

### Добавление новых команд

1.  **Добавьте аргумент:** В `CommandLineProcessor` добавьте парсинг нового аргумента командной строки.
2.  **Обновите координацию:** В `ClientApplication` используйте данные из `CommandLineProcessor` для вызова соответствующих методов `ClipboardBridgeService`.
3.  **Реализуйте логику:** При необходимости добавьте новые методы в `ClipboardBridgeService` и его зависимости (доменные сервисы, инфраструктурные компоненты).

### Расширение функциональности

При расширении функциональности соблюдайте принципы гексагональной архитектуры:
1. Доменная логика должна быть изолирована от внешних зависимостей
2. Внешние зависимости должны адаптироваться к интерфейсам предметной области
3. Новые порты должны определяться в доменном слое
4. Адаптеры должны реализовывать порты в инфраструктурном слое
