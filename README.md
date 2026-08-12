# End-to-End Encrypted Messenger

Клиент-серверный мессенджер: Go-бэкенд работает как каталог ключей и ретранслятор
зашифрованных сообщений, само шифрование (Double Ratchet) реализовано на клиентах — сервер
никогда не видит открытый текст. Два клиента на одном gRPC-контракте: веб (JavaScript) и
Android (Kotlin, Jetpack Compose).

## Возможности

- gRPC API + встроенный grpc-web-прокси в одном процессе — браузер и Android-клиент работают
  по общему контракту (`api/proto/messenger/v1/messenger.proto`)
- Регистрация, профили, поиск пользователей, личные и групповые беседы, потоковая доставка
  событий (`StreamEvents`, server-streaming)
- Публикация и раздача identity keys, signed и one-time prekeys для инициализации
  Double Ratchet на стороне клиентов
- Медиа и history-архив шифруются на клиенте — сервер хранит только зашифрованные данные
- Хранилище за интерфейсами (`UserStore`, `MessageStore`, `ConversationStore`, `KeyStore`,
  `ArchiveStore`) с реализациями на PostgreSQL (pgx) и in-memory (используется в тестах)
- Интеграционные тесты хранилища против настоящего PostgreSQL через testcontainers
- Multi-stage Docker-сборка (веб-фронт → Go-бинарь → alpine) и docker-compose с healthcheck

## Быстрый старт

```bash
make up      # docker compose up -d --build: Postgres + сервер
make logs
make down
```

Сервер слушает `:8082` (HTTP / grpc-web) и `:9090` (gRPC).

Локальная разработка без Docker:

```bash
make proto           # генерация Go-кода из .proto
go run ./cmd/server   # DATABASE_URL не задан → используется in-memory store
```

## Архитектура

```
cmd/server        — точка входа: выбор хранилища, запуск gRPC- и grpc-web-серверов
internal/auth      — регистрация, вход, сессии
internal/grpcapi     — реализация gRPC-сервисов (Auth / User / Message)
internal/store        — интерфейсы хранилищ + реализации на PostgreSQL и in-memory
api/proto              — контракт messenger.v1
web/                     — веб-клиент: UI, Double Ratchet, шифрование медиа и архива
messenger/                — Android-клиент (Kotlin, Jetpack Compose)
```

Сервер — каталог ключей и ретранслятор: хранит identity keys и prekey-бандлы, доставляет
зашифрованные сообщения между клиентами через `StreamEvents`, но не участвует в самом
шифровании. Double Ratchet, инициализация сессии по prekey bundle и шифрование медиа/архива
реализованы в `web/src/lib` на JavaScript и зеркально на Kotlin в Android-клиенте.

## Технические решения

- **Хранилище за интерфейсами.** Пять доменных сторов, у каждого — реализация на PostgreSQL
  для прода и in-memory для юнит-тестов без поднятой базы.
- **gRPC и grpc-web в одном сервере.** Браузер не может работать по «чистому» HTTP/2 gRPC,
  поэтому прокси встроен в тот же процесс вместо отдельного Envoy — меньше движущихся частей
  для сервиса такого размера.
- **Сервер как zero-knowledge relay.** Весь протокол шифрования реализован на клиентах;
  сервер хранит только публичные ключи и уже зашифрованные полезные нагрузки.
- **testcontainers для интеграционных тестов хранилища.** Тесты `internal/store` поднимают
  настоящий PostgreSQL в Docker вместо мока, чтобы проверять реальные SQL-запросы и схему,
  а не поведение эмулятора.

## Тестирование

```bash
make test              # go test ./...
make test-integration   # testcontainers + PostgreSQL
make test-web             # cd web && npm test (Vitest)
make cover                 # покрытие auth / grpcapi / store
```

## Известные ограничения и планы

- Сессии хранятся в памяти без TTL и не переживают рестарт сервера — планируется вынести в
  PostgreSQL или Redis.
- Пароли хешируются SHA-256 с солью — в планах переход на Argon2id.
- CORS для grpc-web сейчас разрешает любой origin — сузить списком через конфиг.
- Нет rate limiting на аутентификации.
- Graceful shutdown для gRPC/HTTP-серверов ещё не реализован.
- CI-пайплайн пока не настроен — линтер и тесты гоняются локально.
