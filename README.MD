# OTP Service

Это приложение предоставляет api для генерации и отправки OTP кодов

## Запуск

1. Склонировать репозиторий на свой компьютер.
2. Открыть проект в **IntelliJ IDEA**.
3. Создать свою бд  Postgres и суперюзера для нее.
4. В application.properties поменять все параметры под свои данные (бд, email, tg, smpp).
5. Создать таблицы c помощью команд:
```
CREATE TABLE users (
   id SERIAL PRIMARY KEY,
   login VARCHAR(50) UNIQUE NOT NULL,
   password_hash VARCHAR(255) NOT NULL,
   role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'USER'))
   );

CREATE TABLE otp_config (
id INT PRIMARY KEY CHECK (id = 1),
code_length INT NOT NULL,
ttl_seconds INT NOT NULL
);

CREATE TABLE otp_codes (
id SERIAL PRIMARY KEY,
user_id INT REFERENCES users(id) ON DELETE CASCADE,
operation_id VARCHAR(100),
code VARCHAR(10) NOT NULL,
status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'EXPIRED', 'USED')),
created_at TIMESTAMP DEFAULT NOW(),
expires_at TIMESTAMP
);
```
6. Сбилдить и запустить проект. 
7. Для корректной работы необходима **Java версии 21 или выше** и Gradle.

## Как пользоваться приложением

После старта приложения можно выполнять запросы для работы с otp кодом

### API Регистрации

- **Пример запрос-ответ, для регистрации юзера**:
```
curl --request POST \
--url http://localhost:8080/user/register \
--header 'Content-Type: application/json' \
--header 'User-Agent: insomnia/10.3.0' \
--data '{
"login": "user",
"password": "123456",
"role": "user"
}'
```
```
{"message":"User registered successfully","status":"success"}
```
- **Пример запрос-ответ, для регистрации админа**:
```
curl --request POST \
--url http://localhost:8080/user/register \
--header 'Content-Type: application/json' \
--header 'User-Agent: insomnia/10.3.0' \
--data '{
"login": "admin",
"password": "123456",
"role": "admin"
}'
```
```
{"message":"User registered successfully","status":"success"}
```

- **Если логин занят, админ уже создан или указать некорректную роль, вернется код 400 и ошибка**:
```
{"message":"Invalid role","status":"failed"}
```
```
{"message":"Login already taken","status":"failed"}
```
```
{"message":"Registration failed","status":"failed"}
```

### API Логина

- **Пример запрос-ответ, для логина пользователя**:
```
curl --request POST \
  --url http://localhost:8080/user/login \
  --header 'Content-Type: application/json' \
  --header 'User-Agent: insomnia/10.3.0' \
  --data '{
  "login": "user",
  "password": "123456"
}'
```
```
{"expiresAt":"2025-05-08T19:47:13.399Z","token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MSwicm9sZSI6IlVTRVIiLCJpYXQiOjE3NDY3MzAwMzMsImV4cCI6MTc0NjczMzYzM30.zKegB5Knef9xoiJrrgU_rjIMBW4qwJ-wQlcBJiZlMSE"}
```
- **Если ввести неверную пару логин+ пароль, будет 401 код и ошибка**:

```
{"message":"Login failed","status":"failed"}
```

### API OTP

- **Пример запрос-ответ, для генерации OTP**:
```
curl --request POST \
  --url http://localhost:8080/otp/generate \
  --header 'Authorization: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Miwicm9sZSI6IlVTRVIiLCJpYXQiOjE3NDY3MjcyNzcsImV4cCI6MTc0NjczMDg3N30.tB8rE7rE-fFTWdXoxm7iz28yDwlktMIhRHp7HxPT9rA' \
  --header 'Content-Type: application/json' \
  --header 'User-Agent: insomnia/10.3.0' \
  --data '{
    "operation_id": "550e8400-e29b-41d4-a716-446655440000",
    "channels": [
        {
            "type": "email",
            "address": "user@example.com"
        },
        {
            "type": "sms",
            "address": "+1234567890"
        },
        {
            "type": "telegram",
            "address": "123456789"
        }
    ]
}'
```
```
{"code":"869798"}
```
**При успешной генерации кода, произойдет попытка отправки его по указанным каналам в поле channels. Если канал неизвестен или произойдет ошибка при отправке, будет добавлена запись с кодом в файл**

**При генерации, у кода будет статус ACTIVE. Если его не "провалидироовать" в срок жизни, он автоматически перейдет в статус EXPIRED. Срок жизни и длина кода берется из бд, если настроек нет, то из пропертей проекта.**
- **Если не указать корректный токен в Authorization(его получаем при логине), То будет 401 ошибка**:
```
{"error":"Unauthorized"}
```

- **Пример запрос-ответ, для валидации OTP**:
```
curl --request POST \
  --url http://localhost:8080/otp/validate \
  --header 'Authorization: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MSwicm9sZSI6IlVTRVIiLCJpYXQiOjE3NDY3MzA1NDMsImV4cCI6MTc0NjczNDE0M30.JKn8AK8I60aDc1oolYtr41HQQp5P7EYiaYVlVbpN_Qk' \
  --header 'Content-Type: application/json' \
  --header 'User-Agent: insomnia/10.3.0' \
  --data '{
	"operation_id": "753b1e2d-3c32-4d00-822d-2f6aea5968bd",
  "code": "281641"
}'
```
```
{"valid":true}
```
**При успешной валидации кода, он автоматически перейдет в статус USED**
- **Если указать неверную пару code + operation_id, то будет ответ**:
```
{"valid":false}
```

- **Если не указать корректный токен в Authorization(его получаем при логине), то будет 401 ответ с пустым телом**

### API ADMIN

- **Пример запрос-ответ, для обновления конфигов**:
```
curl --request POST \
  --url http://localhost:8080/admin/config \
  --header 'Authorization: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Mywicm9sZSI6IkFETUlOIiwiaWF0IjoxNzQ2NzMwODMwLCJleHAiOjE3NDY3MzQ0MzB9.nrcC5jCuEzb270JOFAqpqvas8jMG4XlmIYgfshdsmFA' \
  --header 'Content-Type: application/json' \
  --header 'User-Agent: insomnia/10.3.0' \
  --data '{
	"code_length": 6,
	"ttl_seconds": 60
}'
```
```
{"message":"Configuration updated successfully","status":"success"}
```
- **Если не указать токен админа в Authorization(его получаем при логине), то будет 403 ошибка без тела**

- **Пример запрос-ответ, для удаления юезра**:
```
curl --request DELETE \
  --url http://localhost:8080/admin/delete-user/ \
  --header 'Authorization: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6Mywicm9sZSI6IkFETUlOIiwiaWF0IjoxNzQ2NzMwODMwLCJleHAiOjE3NDY3MzQ0MzB9.nrcC5jCuEzb270JOFAqpqvas8jMG4XlmIYgfshdsmFA' \
  --header 'Content-Type: application/json' \
  --header 'User-Agent: insomnia/10.3.0' \
  --data '{
	user_id: 1
}'
```
```
{"message":"Пользователь успешно удален","status":"success"}
```
- **Если указать несуществующий user_id, то будет успешный ответ с другим сообщением**:
```
{"message":"Пользователь не удален","status":"success"}
```

- **Если указать user_id админа, то будет 500**:
```
{"message":"Невозможно удалить администратора","status":"failed"}
```

- **Если не указать токен админа в Authorization(его получаем при логине), то будет 403 ошибка без тела**