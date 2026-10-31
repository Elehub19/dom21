# star-recommendation

Рекомендательный сервис банка «Стар». Spring Boot 3, Java 17.

- Фиксированные рекомендации (Invest 500, Top Saving, Простой кредит) — по базе клиентов (H2, только чтение, `JdbcTemplate`).
- Динамические правила рекомендаций — REST API `/rule`, хранятся в PostgreSQL (JPA + Liquibase).
- Статистика срабатываний динамических правил — `GET /rule/stats`.
- Telegram-бот: команда `/recommend username`.
- Результаты SQL-запросов к базе клиентов кешируются (Caffeine); кеш сбрасывается `POST /management/clear-caches`.
- `GET /management/info` — имя и версия сервиса из `pom.xml`.

## Запуск

### 1. База для правил

Вариант А — PostgreSQL (по умолчанию, настройки в `application.properties`):

```
docker compose up -d
```

Поднимется PostgreSQL 16 с базой `star_recommendation` (пользователь и пароль `star`).
Таблицы создаст Liquibase при старте приложения.

Вариант Б — без PostgreSQL, правила хранятся в памяти (теряются при перезапуске): запустите приложение с профилем `h2`
(в IntelliJ: Run → Edit Configurations → Active profiles: `h2`; из консоли: `-Dspring.profiles.active=h2`).

### 2. Telegram-бот (необязательно)

1. Создайте бота у [@BotFather](https://t.me/BotFather) командой `/newbot` и получите токен.
2. Передайте токен приложению: переменная окружения `TELEGRAM_BOT_TOKEN`
   (в IntelliJ: Run → Edit Configurations → Environment variables: `TELEGRAM_BOT_TOKEN=123456:ABC...`).
   Либо создайте файл `src/main/resources/application-local.properties` со строкой `telegram.bot.token=...`
   и добавьте профиль `local` (файл в `.gitignore`, в репозиторий он не попадёт).

Без токена сервис работает как раньше, просто без бота.

### 3. Приложение

Файл `transaction.mv.db` (база клиентов) лежит в корне проекта, приложение нужно запускать из этой папки
(в IDE — рабочая директория = корень проекта).

```
mvn spring-boot:run
```

Если путь к проекту содержит кириллицу, запускайте из IDE (класс `RecommendationApplication`)
или добавьте `-Dspring-boot.run.fork=false`.

Сборка JAR: `mvn package` (целиком собирает проект и генерирует `build-info.properties`).
Тесты: `mvn test`.

## Telegram-бот

- При первом сообщении (и по `/start`, `/help`) бот приветствует пользователя и печатает справку.
- `/recommend username` — рекомендации для пользователя с таким `username` из базы клиентов:

```
Здравствуйте Иван Петров

Новые продукты для вас:

• Invest 500
Откройте свой путь к успеху с индивидуальным инвестиционным счетом (ИИС)...

• Top Saving
...
```

- Если пользователь не найден или найдено несколько пользователей — ответ «Пользователь не найден».
- Если рекомендаций нет — «Здравствуйте Иван Петров» и сообщение, что новых продуктов пока нет.
- Правила выдачи те же, что в REST API (фиксированные и динамические). Длинный ответ делится на несколько сообщений
  (лимит Telegram — 4096 символов).

Логика бота (`BotService`) не зависит от Telegram и покрыта модульными тестами; `TelegramBotListener` только принимает
сообщения и отправляет ответы.

## API

### Рекомендации

`GET /recommendation/{user_id}` →

```json
{
  "user_id": "cd515076-5d8a-44be-930e-8d4fcb79f42d",
  "recommendations": [
    {"name": "Invest 500", "id": "147f6a0f-3b91-413b-ab99-87f081d60d5a", "text": "..."}
  ]
}
```

Если подходящих продуктов нет, `recommendations` — пустой массив. В ответ попадают и фиксированные, и динамические
рекомендации; один продукт не повторяется (при совпадении `id` побеждает фиксированное правило).
Несколько динамических правил с одним `product_id` работают как «ИЛИ».

### Динамические правила

| Метод | Путь | Ответ |
|-------|------|-------|
| POST | `/rule` | 200, созданное правило с присвоенным `id` |
| GET | `/rule` | 200, `{"data": [ ...правила... ]}` |
| GET | `/rule/stats` | 200, статистика срабатываний всех правил |
| DELETE | `/rule/{id}` | 204 No Content |

`DELETE` принимает либо `id` правила (число), либо `product_id` продукта (UUID) — в этом случае удаляются все правила
этого продукта. Если правила нет, ответ всё равно 204. Статистика удаляется вместе с правилом.

Тело `POST /rule` — как в ТЗ (`product_name`, `product_id`, `product_text`, `rule`). Некорректное правило
(неизвестный тип запроса, не то число аргументов, неверный тип продукта/транзакции/операции, отрицательная или слишком
большая константа) отклоняется с кодом 400 и `{"error": "..."}`.

Типы запросов:

| query | arguments |
|-------|-----------|
| `USER_OF` | `[тип продукта]` — есть хотя бы одна транзакция |
| `ACTIVE_USER_OF` | `[тип продукта]` — не меньше 5 транзакций |
| `TRANSACTION_SUM_COMPARE` | `[тип продукта, тип транзакции, операция, константа]` |
| `TRANSACTION_SUM_COMPARE_DEPOSIT_WITHDRAW` | `[тип продукта, операция]` — пополнения сравниваются с тратами |

Типы продуктов: `DEBIT`, `CREDIT`, `INVEST`, `SAVING`. Типы транзакций: `DEPOSIT`, `WITHDRAW`.
Операции: `>`, `<`, `=`, `>=`, `<=`. Правило выполняется, если все запросы вернули `true` (с учётом `negate`).

### Статистика срабатываний

`GET /rule/stats` →

```json
{
  "stats": [
    {"rule_id": 1, "count": 12},
    {"rule_id": 2, "count": 0}
  ]
}
```

Каждый раз, когда динамическое правило выполняется для пользователя (в REST-запросе или в боте), счётчик правила
увеличивается на 1. В списке есть все правила, в том числе без срабатываний (`count: 0`).

- Счётчик хранится в таблице `rule_stats` (сущность `RuleStatEntity`, связь `@OneToOne` с правилом).
  Нулевой счётчик создаётся вместе с правилом, для уже существующих правил — миграцией Liquibase.
- Инкремент атомарный: один SQL `UPDATE rule_stats SET hit_count = hit_count + 1 WHERE rule_id = ?`,
  значение не читается в приложение, поэтому параллельные запросы не теряют срабатывания.
- При удалении правила счётчик удаляется каскадно (`ON DELETE CASCADE` во внешнем ключе), в той же операции.

### Управление

| Метод | Путь | Описание |
|-------|------|----------|
| POST | `/management/clear-caches` | Без тела. Сбрасывает все кеши (200 OK); следующие запросы снова идут в базу |
| GET | `/management/info` | `{"name": "star-recommendation", "version": "0.0.1-SNAPSHOT"}` |

Имя и версия берутся из `pom.xml` через `BuildProperties`: цель `build-info` плагина `spring-boot-maven-plugin`
создаёт `META-INF/build-info.properties` при сборке (`mvn compile` / `mvn package`). Если приложение запущено без такой
сборки (например, IDE не запустила Maven), вернётся имя из `spring.application.name` и версия `unknown`.

### Примеры (PowerShell, из корня проекта)

```powershell
# создать правило (файл отправляется байтами, чтобы не испортить кириллицу)
Invoke-RestMethod -Method Post -Uri http://localhost:8080/rule `
  -ContentType 'application/json; charset=utf-8' `
  -Body ([System.IO.File]::ReadAllBytes('docs/examples/rule-active-debit.json'))

# список правил и статистика
Invoke-RestMethod http://localhost:8080/rule | ConvertTo-Json -Depth 10
Invoke-RestMethod http://localhost:8080/rule/stats | ConvertTo-Json -Depth 10

# сбросить кеши
Invoke-RestMethod -Method Post -Uri http://localhost:8080/management/clear-caches

# информация о сервисе
Invoke-RestMethod http://localhost:8080/management/info

# удалить правило по id
Invoke-RestMethod -Method Delete -Uri http://localhost:8080/rule/1

# или по id продукта
Invoke-RestMethod -Method Delete -Uri http://localhost:8080/rule/11111111-2222-3333-4444-555555555555
```

То же через curl: `curl.exe -X POST http://localhost:8080/rule -H "Content-Type: application/json" --data-binary "@docs/examples/rule-active-debit.json"`.

После создания `rule-active-debit.json` для большинства пользователей в `/recommendation/{user_id}` появится «Premium-карта»,
а счётчик этого правила в `/rule/stats` вырастет.

## Кеширование

`TransactionsRepository` — два метода, два кеша Caffeine (паттерн get-if-absent-compute):

| Метод | Ключ кеша |
|-------|-----------|
| `countTransactions(userId, productType)` | `userId + productType` |
| `sumAmount(userId, productType, transactionType)` | `userId + productType + transactionType` |

`USER_OF` и `ACTIVE_USER_OF` используют один и тот же метод подсчёта (`> 0` и `>= 5`), оба сравнения сумм — метод `sumAmount`.
База клиентов не меняется, поэтому записи не истекают, их можно сбросить только `POST /management/clear-caches`;
размер каждого кеша ограничен 10 000 записей.

Проверка: дважды запросите `/recommendation/{user_id}` для одного пользователя — второй ответ должен прийти быстрее;
после `POST /management/clear-caches` следующий запрос снова медленнее.

## Архитектура

- `config.DataSourceConfiguration` — две БД: основная (`spring.datasource.*`, PostgreSQL, `@Primary`; JPA и Liquibase)
  и база клиентов H2 (`application.recommendations-db.*`, read-only, своя `JdbcTemplate`).
- `controller` — `RecommendationController`, `RuleController`, `ManagementController`, `ApiExceptionHandler`.
- `bot` — `BotService` (логика), `TelegramBotListener` + `TelegramBotConfiguration` (подключение к Telegram).
- `service.RecommendationService` — фиксированные `RecommendationRuleSet` + `RuleRepository.findAll()`,
  проверка динамических правил и учёт срабатываний.
- `service.RuleService` — создание/список/удаление правил, валидация; `service.RuleStatsService` — счётчики.
- `dynamic` — `RuleQueryHandler` (по одному `@Component` на тип запроса) и `DynamicRuleEvaluator`.
  Новый тип запроса = значение в `QueryType` + новый обработчик.
- `entity` / `repository` — JPA-сущности правил (`rules`, `rule_queries`, `rule_query_arguments`) и счётчиков (`rule_stats`).
- `repository.TransactionsRepository` и `repository.UsersRepository` — запросы к базе клиентов; первый кеширует результаты.
- Миграции: `src/main/resources/db/changelog`.

## Схема базы клиентов

`TRANSACTIONS(ID, PRODUCT_ID, USER_ID, TYPE, AMOUNT)`, `PRODUCTS(ID, TYPE, NAME)`, `USERS(ID, USERNAME, FIRST_NAME, LAST_NAME)`.
