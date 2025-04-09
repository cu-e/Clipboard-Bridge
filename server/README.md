# Clipboard Bridge - Сервер

Серверная часть приложения Clipboard Bridge для обмена содержимым буфера обмена.


### Структура проекта

```
src/main/java/io/github/cue/clipboardbridge/server/
├── application           # Слой приложения
│   └── ServerApplication # Точка входа в приложение
├── domain                # Доменный слой
│   ├── model             # Доменные модели
│   ├── port              # Порты для взаимодействия с внешними системами (NotificationService)
│   └── service           # Доменные сервисы (CommandProcessorService)
├── infrastructure        # Инфраструктурный слой
│   ├── adapter           # Адаптеры для внешних систем (TelegramBotAdapter, TelegramNotificationService)
│   ├── config            # Конфигурации Spring (TelegramBotConfig)
│   ├── port              # Порты для инфраструктурных компонентов (TelegramBotApi, TelegramUpdateListener)
│   ├── controller        # WebSocket контроллеры (WebSocketController)
│   └── service           # Инфраструктурные сервисы (ClientSessionService, WebSocketSessionMessageService)
```

## Запуск сервера

### С использованием Docker (рекомендуется)

Самый простой способ запустить сервер - использовать Docker Compose из корневой директории проекта.

1. Настройте переменные окружения в файле `.env` в корневой директории проекта:
   ```
   TELEGRAM_BOT_TOKEN=ваш_токен_бота
   TELEGRAM_BOT_USERNAME=имя_вашего_бота
   TELEGRAM_MAIN_USER_ID=ваш_id_в_telegram
   ```

2. Запустите сервер:
   ```bash
   docker-compose up -d server
   ```

3. Проверьте логи:
   ```bash
   docker-compose logs -f server
   ```

### Ручной запуск

#### Предварительные требования

- JDK 17 или выше
- Gradle 8.0 или выше

#### Сборка

```bash
./gradlew clean build
```

#### Запуск

```bash
# Использование с настройками по умолчанию
java -jar build/libs/server-0.0.1-SNAPSHOT.jar

# Использование с указанием токена Telegram бота
java -jar build/libs/server-0.0.1-SNAPSHOT.jar \
  --telegram.bot.token=ваш_токен_бота \
  --telegram.bot.username=имя_вашего_бота \
  --telegram.main.user.id=ваш_id_в_telegram
```

## API

### WebSocket Endpoints

- **Соединение**: `/ws` (SockJS)
- **Адрес для отправки команд**: `/app/command`
- **Адрес для получения ответов**: `/user/queue/reply`

## Для разработчиков

### Добавление новых команд

1. **Определите команду:** При необходимости расширьте `CommandMessage` или создайте новую модель.
2. **Добавьте обработку:** В `CommandProcessorService` добавьте логику для новой команды, делегируя выполнение соответствующим доменным или инфраструктурным сервисам.
3. **Сервисы/Порты/Адаптеры:** Если требуется взаимодействие с новыми внешними системами, создайте соответствующие порты в `domain/port` или `infrastructure/port` и реализуйте адаптеры в `infrastructure/adapter`.

### Расширение функциональности

Соблюдайте принципы гексагональной архитектуры:
1. Доменная логика должна быть изолирована от внешних зависимостей
2. Внешние зависимости должны адаптироваться к интерфейсам предметной области
3. Новые порты должны определяться в доменном слое
4. Адаптеры должны реализовывать порты в инфраструктурном слое 