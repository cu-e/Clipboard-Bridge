# Clipboard Bridge

Система для обмена содержимым буфера обмена через Telegram.

## Архитектура проекта

Clipboard Bridge состоит из двух основных компонентов:

1. **Сервер** - принимает сообщения от клиентов и отправляет их в Telegram
2. **Клиент** - отправляет содержимое буфера обмена на сервер и получает ответы

## Запуск с использованием Docker

### Предварительные требования

- Docker
- Docker Compose

### Настройка Telegram бота

1. Создайте бота в Telegram с помощью [@BotFather](https://t.me/botfather)
2. Получите токен бота
3. Настройте переменные окружения в файле `.env`:
   ```
   TELEGRAM_BOT_TOKEN=ваш_токен_бота
   TELEGRAM_BOT_USERNAME=имя_вашего_бота
   TELEGRAM_MAIN_USER_ID=ваш_id_в_telegram
   ```

Для получения вашего ID в Telegram отправьте сообщение боту [@userinfobot](https://t.me/userinfobot).

### Запуск сервера

```bash
# Клонирование репозитория
git clone https://github.com/cu-e/clipboard-bridge.git
cd clipboard-bridge

# Запуск сервера с использованием Docker Compose
docker-compose up -d

```

### Остановка сервера

```bash
docker-compose down
```

## Запуск клиента

Клиент запускается отдельно на устройстве, с которого нужно отправить содержимое буфера обмена.

```bash
# Переход в директорию клиента
cd client

# Сборка
./gradlew build

# Запуск с использованием содержимого буфера обмена
java -jar build/libs/client-0.0.1-SNAPSHOT.jar

# Запуск с указанием текста
java -jar build/libs/client-0.0.1-SNAPSHOT.jar -m "Текст для отправки"
```

## Архитектурные принципы

Проект реализован с использованием следующих архитектурных принципов:

- **Гексагональная архитектура (Ports and Adapters)**
- **Чистая архитектура (Clean Architecture)**
- **SOLID, DRY, KISS, YAGNI**
- **Dependency Injection & Inversion of Control**
