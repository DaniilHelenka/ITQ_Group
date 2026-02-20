# Document Service — ITQ Group

Сервис управления документами с переходами по статусам (DRAFT → SUBMITTED → APPROVED), историей изменений, реестром утверждений, пакетными API, фоновыми воркерами и утилитой массового создания документов.

## Стек

- **Java 21**, Spring Boot 3.2
- PostgreSQL 16 (Docker Compose)
- JPA / Hibernate, Liquibase
- Maven (multi-module)

## Структура проекта

```
document-service/            (parent POM)
├── document-service-api/    (DTO, request/response)
├── document-service-core/   (JPA-сущности, репозитории, сервисы)
├── document-service-app/    (REST API, Liquibase, воркеры)
├── document-generator-util/ (утилита создания N документов)
├── docker-compose.yml
└── README.md
```

## Быстрый старт

### 1. Запуск PostgreSQL

```bash
docker-compose up -d
```

PostgreSQL будет доступен на `localhost:5432` (user: `docuser`, password: `docpass`, db: `document_db`).

### 2. Сборка проекта

```bash
mvn clean install
```

### 3. Запуск приложения

```bash
cd document-service-app
mvn spring-boot:run
```

Приложение запустится на `http://localhost:8080`.

### 4. Запуск тестов

```bash
mvn test
```

Тесты используют H2 in-memory (профиль `test`), Docker не требуется.

---

## Локальный запуск и тестирование (пошагово)

Убедитесь, что установлены **JDK 21**, **Maven** и **Docker** (для PostgreSQL).

### Шаг 1. Запустить PostgreSQL

В корне проекта (`ITQ_Group`):

```bash
docker-compose up -d
```

Проверка: `docker ps` — контейнер `itq-postgres` в статусе Up. БД: `document_db`, пользователь `docuser`, пароль `docpass`, порт `5432`.

### Шаг 2. Собрать проект

В корне проекта (обязательно с **JDK 21**):

```bash
mvn clean install
```

В Windows (PowerShell) укажите JDK 21 и соберите из **корня** проекта (`ITQ_Group`):
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
cd C:\Users\User\IdeaProjects\ITQ_Group
mvn clean install
```
Проверка: `& "$env:JAVA_HOME\bin\java.exe" -version` — должна быть версия 21.

### Шаг 3. Запустить приложение

Из корня:

```bash
cd document-service-app
mvn spring-boot:run
```

Либо в IDE: запустить класс `ru.itq.app.DocumentServiceApplication` (main). В логах должно быть `Started DocumentServiceApplication` и порт 8080.

### Шаг 4. Проверить API

Сервис доступен по адресу **http://localhost:8080**.

**Создать документ (PowerShell):**
```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/documents -Method POST -ContentType "application/json" -Body '{"author":"Иванов","title":"Договор №1","initiator":"admin"}'
```

**Или через curl (если установлен):**
```bash
curl -X POST http://localhost:8080/api/documents -H "Content-Type: application/json" -d "{\"author\":\"Иванов\",\"title\":\"Договор №1\",\"initiator\":\"admin\"}"
```

В ответе будет созданный документ со статусом `DRAFT` и полем `documentNumber` (например, `DOC-0000001`). Запомните `id`.

**Получить документ с историей:**
```bash
curl http://localhost:8080/api/documents/1
```

**Отправить на согласование (submit):**
```bash
curl -X POST http://localhost:8080/api/documents/submit -H "Content-Type: application/json" -d "{\"ids\":[1],\"initiator\":\"admin\"}"
```

**Утвердить (approve):**
```bash
curl -X POST http://localhost:8080/api/documents/approve -H "Content-Type: application/json" -d "{\"ids\":[1],\"initiator\":\"admin\"}"
```

После approve снова запросите `GET /api/documents/1` — статус должен быть `APPROVED`, в `history` — записи SUBMIT и APPROVE.

### Шаг 5. Утилита массового создания документов

Собрать (если ещё не делали): из корня `mvn clean install`. Затем:

```bash
java -jar document-generator-util/target/document-generator-util-1.0.0-SNAPSHOT.jar
```

По умолчанию создаётся 100 документов (см. `document-generator-util/src/main/resources/generator-config.properties`). Свой конфиг:

```properties
generator.count=50
generator.base-url=http://localhost:8080/api/documents
```

Сохраните в файл `my-config.properties` и запустите:

```bash
java -jar document-generator-util/target/document-generator-util-1.0.0-SNAPSHOT.jar my-config.properties
```

В логах будет прогресс (например, каждые 100 документов) и итоговое время.

### Шаг 6. Фоновые воркеры

После запуска приложения воркеры работают автоматически (интервал в `document-service-app/src/main/resources/application.yml`: `submit-delay`, `approve-delay` — по умолчанию 30 сек). Они забирают документы в статусе DRAFT и SUBMITTED пачками (`batch-size: 50`) и переводят их по статусам. В логах приложения ищите строки вида:

- `SUBMIT-worker: processing batch of ...`
- `APPROVE-worker: batch completed — processed=...`

### Остановка

- Остановить приложение: в терминале с `spring-boot:run` — **Ctrl+C**.
- Остановить PostgreSQL: в корне проекта `docker-compose down` (данные сохраняются в volume `pgdata`; при `docker-compose down -v` volume удалится).

## API

| Метод | URL | Описание |
|-------|-----|----------|
| POST | `/api/documents` | Создание документа (DRAFT) |
| GET | `/api/documents/{id}` | Получение документа с историей |
| GET | `/api/documents?ids=1,2,3&page=0&size=20` | Пакетное получение по ID |
| POST | `/api/documents/submit` | Пакетный submit (DRAFT → SUBMITTED) |
| POST | `/api/documents/approve` | Пакетный approve (SUBMITTED → APPROVED) |
| GET | `/api/documents/search?status=DRAFT&author=...&dateFrom=...&dateTo=...` | Поиск с фильтрами |
| POST | `/api/documents/concurrent-approve-test` | Тест конкурентного утверждения |

### Примеры запросов

**Создание документа:**
```bash
curl -X POST http://localhost:8080/api/documents \
  -H "Content-Type: application/json" \
  -d '{"author": "Иванов", "title": "Договор №1", "initiator": "admin"}'
```

**Пакетный submit:**
```bash
curl -X POST http://localhost:8080/api/documents/submit \
  -H "Content-Type: application/json" \
  -d '{"ids": [1, 2, 3], "initiator": "admin"}'
```

**Поиск по периоду (по дате создания):**
```bash
curl "http://localhost:8080/api/documents/search?status=DRAFT&dateFrom=2025-01-01T00:00:00&dateTo=2025-12-31T23:59:59&page=0&size=20&sort=createdAt,desc"
```

> **Важно:** Период в поиске фильтрует по полю `createdAt` (дата создания документа).

## Фоновые воркеры

- **SUBMIT-worker** — каждые 30 секунд выбирает до `batchSize` документов в статусе DRAFT и переводит в SUBMITTED.
- **APPROVE-worker** — каждые 30 секунд выбирает до `batchSize` документов в статусе SUBMITTED и переводит в APPROVED.

Конфигурация в `application.yml`:
```yaml
app:
  worker:
    batch-size: 50
    submit-delay: 30000    # мс
    approve-delay: 30000   # мс
```

Воркеры используют `SELECT ... FOR UPDATE SKIP LOCKED` для исключения двойной обработки.

### Примеры логов воркеров

```
INFO  SUBMIT-worker: processing batch of 50 documents, ~150 DRAFT remaining
INFO  SUBMIT-worker: batch completed — processed=50, success=50, elapsed=320ms
INFO  APPROVE-worker: processing batch of 50 documents, ~100 SUBMITTED remaining
INFO  APPROVE-worker: batch completed — processed=50, success=50, elapsed=450ms
```

## Утилита генерации документов

### Сборка

```bash
mvn clean package
```

### Запуск (с конфигом по умолчанию — 100 документов)

```bash
java -jar document-generator-util/target/document-generator-util-1.0.0-SNAPSHOT.jar
```

### Запуск с пользовательским конфигом

Создайте файл `my-config.properties`:
```properties
generator.count=1000
generator.base-url=http://localhost:8080/api/documents
```

```bash
java -jar document-generator-util/target/document-generator-util-1.0.0-SNAPSHOT.jar my-config.properties
```

### Примеры логов утилиты

```
INFO  Requested 1000 documents, target URL: http://localhost:8080/api/documents
INFO  Progress: created 100 of 1000 (success=100, errors=0)
INFO  Progress: created 200 of 1000 (success=200, errors=0)
...
INFO  Completed: created 1000 of 1000 documents in 12345 ms (errors=0)
```

## Формат ошибок

Все ошибки возвращаются в едином формате:
```json
{
  "code": "DOCUMENT_NOT_FOUND",
  "message": "Document not found: id=999",
  "timestamp": "2025-01-15T10:30:00"
}
```

Коды: `DOCUMENT_NOT_FOUND`, `INVALID_STATUS_TRANSITION`, `REGISTRY_ERROR`, `VALIDATION_ERROR`, `CONCURRENT_MODIFICATION`, `INTERNAL_ERROR`.

## Тесты

| Сценарий | Описание |
|----------|----------|
| Happy-path | Создание → submit → approve, проверка статуса и истории |
| Пакетный submit | Частичный успех (один id не существует) |
| Пакетный approve | Частичный успех (один документ в неверном статусе) |
| Откат approve | Повторное утверждение → конфликт, документ не повреждён |
| Конкурентное утверждение | 10 попыток в 5 потоках → ровно 1 success |

## Масштабирование до 5000+ id

- Разбиение списка на чанки по 1000 id.
- Асинхронная обработка чанков (CompletableFuture).
- Ограничение размера ответа, потоковая обработка с пагинацией.
- Индексы на status, author, created_at; настройка пула соединений HikariCP.

## Реестр в отдельной системе

- **Отдельная БД:** два DataSource, распределённая транзакция (JTA/2PC) или паттерн Outbox.
- **Отдельный HTTP-сервис:** вызов после approve с компенсацией (откат документа при ошибке ответа реестра), Saga-паттерн.
