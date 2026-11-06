# HTTP handlers существующих сервисов

Handlers предоставляют HTTP-доступ к функционалу `cps-core`, описанному
в [перечне функций](backend-functionality.md). Префикс — `/api`.
Тела запросов и ответы — JSON; экспорт возвращает ZIP с CSV-файлами.
Идентификаторы — UUID, даты/время в ответах — ISO 8601 UTC.

## Аргументы сервисов

HTTP-аутентификация, сессии, cookie и определение текущего пользователя
не используются. Там, где метод сервиса принимает идентификатор участника
операции, клиент передаёт этот идентификатор явно:

| Операция | Аргумент | Передача |
| --- | --- | --- |
| Создать/сохранить заказ | `userId` | Тело `OrderDto.Request` |
| Изменить примечание | `userId` | Тело `OrderDto.NotesRequest` |
| Добавить позицию | `userId` | Тело `OrderDto.AddItemRequest` |
| Изменить количество позиции | `userId` | Тело `OrderDto.QuantityRequest` |
| Удалить позицию | `userId` | Query-параметр |
| Сменить статус заказа | `managerId` | Тело `OrderDto.StatusRequest` |
| Удалить пользователя | `actingUserId` | Query-параметр |
| Экспортировать данные | `managerId` | Query-параметр |

Это обычные аргументы вызова. Существующие проверки владельца заказа,
статуса, роли менеджера и запрета удалить себя выполняются в `cps-core`.
Handlers преобразуют запросы в вызовы сервисов, результаты — в DTO,
а ошибки сервисов — в HTTP-ответы. Чтение данных и изменение справочников
не требуют дополнительных идентификаторов участника операции.

## Пользователи — UsersService

| Метод и маршрут | Тело/параметры | Результат |
| --- | --- | --- |
| `GET /api/users` | `q`, `role`, `sort` | `200 UserDto[]` |
| `GET /api/users/{id}` | — | `200 UserDto` |
| `GET /api/users/by-username` | Обязательный `username` | `200 UserDto` |
| `POST /api/users` | `UserDto.Request` | `201 UserDto`, `Location` |
| `PUT /api/users/{id}` | `UserDto.Request` | `200 UserDto` |
| `DELETE /api/users/{id}` | Обязательный `actingUserId` | `204` |

Поиск по имени вызывает существующий `UsersService.login(username)`
и возвращает DTO найденного пользователя. Сессия при этом не создаётся;
неизвестное имя возвращает `404`. Имя очищается от пробелов по краям
и сравнивается с учётом регистра, как в сервисе.

`role`: `USER` или `MANAGER`. `sort`: `NAME_ASC`, `NAME_DESC`, `CREATED_ASC`,
`CREATED_DESC` (по умолчанию). `q` ищет по части имени/ID без учёта регистра.
Сервис удаления проверяет действующего пользователя, роль и запрет удалить себя.

## Каталог — ProductsService

| Метод и маршрут | Тело/параметры | Результат |
| --- | --- | --- |
| `GET /api/products` | `q`, `categoryId`, `minPrice`, `maxPrice`, `sort` | `200 ProductDto[]` |
| `GET /api/products/{id}` | — | `200 ProductDto` |
| `POST /api/products` | `ProductDto.Request` | `201 ProductDto`, `Location` |
| `PUT /api/products/{id}` | `ProductDto.Request` | `200 ProductDto` |
| `DELETE /api/products/{id}` | — | `204` |

`q` ищет по ID, названию или артикулу без учёта регистра. `sort`: `NAME_ASC`
(по умолчанию), `NAME_DESC`, `PRICE_ASC`, `PRICE_DESC`. Границы цены включаются.
Сервис запрещает удалить товар из истории заказов; неиспользуемый товар
удаляется вместе с его складскими остатками.

## Категории — CategoriesService

| Метод и маршрут | Тело/параметры | Результат |
| --- | --- | --- |
| `GET /api/categories` | `q` по названию | `200 CategoryDto[]`, включая `productCount` |
| `GET /api/categories/{id}` | — | `200 CategoryDto` |
| `POST /api/categories` | `CategoryDto.Request` | `201 CategoryDto`, `Location` |
| `PUT /api/categories/{id}` | `CategoryDto.Request` | `200 CategoryDto` |
| `DELETE /api/categories/{id}` | — | `204` |

Список сортируется по названию. Сервис запрещает удалить категорию с товарами.

## Склады и остатки — WarehousesService, InventoriesService, StockService

| Метод и маршрут | Тело/параметры | Результат |
| --- | --- | --- |
| `GET /api/warehouses` | `q` по названию/адресу | `200 WarehouseDto[]` |
| `GET /api/warehouses/{id}` | — | `200 WarehouseDto` |
| `POST /api/warehouses` | `WarehouseDto.Request` | `201 WarehouseDto`, `Location` |
| `PUT /api/warehouses/{id}` | `WarehouseDto.Request` | `200 WarehouseDto` |
| `DELETE /api/warehouses/{id}` | — | `204` |
| `GET /api/inventory` | Необязательные `productId`, `warehouseId` | `200 InventoryDto[]` |
| `GET /api/inventory/{id}` | — | `200 InventoryDto` |
| `GET /api/warehouses/{warehouseId}/inventory` | — | `200 InventoryDto[]` |
| `PUT /api/warehouses/{warehouseId}/inventory/{productId}` | `InventoryDto.QuantityRequest` | `201 InventoryDto` при создании, иначе `200` |
| `DELETE /api/inventory/{id}` | — | `204` |
| `GET /api/inventory/availability` | Необязательный `editingOrderId` | `200 InventoryDto.Availability[]` |

Склады сортируются по названию. Запись остатка содержит ID/названия склада
и товара, артикул и физическое количество. Установка количества абсолютная.
Создание записи возвращает `Location`.

Расчёт доступности вызывает `StockService.getAvailableQuantities(editingOrderId)`:
учитываются резервы `CREATED`/`APPROVED`, а резерв указанного редактируемого
заказа исключается по правилам существующего сервиса. Для товаров без запаса
возвращается `0`. Проверки остатков и блокировки при изменениях сохраняются.

## Заказы — OrdersService, OrderItemsService, AuditLogsService

| Метод и маршрут | Тело/параметры | Результат |
| --- | --- | --- |
| `GET /api/orders` | `userId`, `q`, `status`, `dateFrom`, `dateTo`, `minTotal`, `maxTotal`, `sort` | `200 OrderDto[]` |
| `GET /api/orders/{id}` | — | `200 OrderDto`, включая позиции |
| `GET /api/orders/{id}/items` | — | `200 OrderDto.Item[]` |
| `GET /api/orders/{id}/audit` | — | `200 AuditLogDto[]`, по времени изменения |
| `POST /api/orders` | `OrderDto.Request` | `201 OrderDto`, `Location` |
| `PUT /api/orders/{id}` | `OrderDto.Request` | `200 OrderDto` |
| `PATCH /api/orders/{id}/notes` | `OrderDto.NotesRequest` | `200 OrderDto` |
| `POST /api/orders/{id}/items` | `OrderDto.AddItemRequest` | `200 OrderDto` |
| `PUT /api/orders/{id}/items/{itemId}` | `OrderDto.QuantityRequest` | `200 OrderDto` |
| `DELETE /api/orders/{id}/items/{itemId}` | Обязательный `userId` | `200 OrderDto` |
| `PATCH /api/orders/{id}/status` | `OrderDto.StatusRequest` | `200 OrderDto` |
| `DELETE /api/orders/{id}` | — | `204` |

При `userId` вызывается `OrdersService.getAllByUser`, иначе — `getAll`.
`q` ищет по ID/примечанию. Даты — `YYYY-MM-DD`, границы включаются
и сравниваются с датой создания в UTC. `sort`: `CREATED_DESC` (по умолчанию),
`CREATED_ASC`, `TOTAL_ASC`, `TOTAL_DESC`.

Ответ содержит `lineCount` (число строк), `totalQuantity` (число единиц),
`total` (сумма по ценам покупки) и позиции. Заказ создаётся с переданным
`userId` в статусе `CREATED`; цены новых позиций берутся сервисом из каталога.
Сервис проверяет владельца при редактировании, статус `CREATED`, наличие товара
и минимум одну позицию. Смена статуса вызывает `changeStatus(managerId, id, status)`
с существующими проверками роли, доставки и автоматической записью аудита.
Удаление заказа вызывает существующий метод без идентификатора участника:
удаляются позиции и аудит, списанные при доставке остатки не возвращаются.

## Статистика и экспорт — StatisticsService, CsvExportService

| Метод и маршрут | Параметры | Результат |
| --- | --- | --- |
| `GET /api/statistics` | — | `200 StatisticsDto` |
| `GET /api/statistics/warehouses` | — | `200 StatisticsDto.WarehouseStock[]` |
| `GET /api/exports/csv` | Обязательный `managerId` | `200 application/zip`, восемь CSV |

Экспорт передаёт `managerId` в существующий сервис, который проверяет роль.
Архив содержит `users.csv`, `categories.csv`, `products.csv`, `warehouses.csv`,
`inventories.csv`, `orders.csv`, `order_items.csv`, `audit_logs.csv`.
Временные файлы удаляются после подготовки архива.

## DTO запросов

DTO находятся в [пакете dto](../cps-backend/src/main/java/com/team/corporate/backend/dto).

| DTO | Пример JSON |
| --- | --- |
| `UserDto.Request` | `{"username":"ivan_sid","role":"USER"}` |
| `CategoryDto.Request` | `{"name":"Офисная техника"}` |
| `WarehouseDto.Request` | `{"name":"Основной","location":null}` |
| `ProductDto.Request` | `{"name":"Принтер","sku":"PRINTER-001","categoryId":"<UUID>","price":12500.00}` |
| `InventoryDto.QuantityRequest` | `{"quantity":10}` |
| `OrderDto.Request` | `{"userId":"<UUID>","items":[{"productId":"<UUID>","quantity":2}],"notes":"Для отдела"}` |
| `OrderDto.AddItemRequest` | `{"userId":"<UUID>","productId":"<UUID>","quantity":1}` |
| `OrderDto.QuantityRequest` | `{"userId":"<UUID>","quantity":3}` |
| `OrderDto.NotesRequest` | `{"userId":"<UUID>","notes":null}` |
| `OrderDto.StatusRequest` | `{"managerId":"<UUID>","status":"APPROVED"}` |

В `PUT` склада `location:null` очищает адрес. В изменении примечания `notes:null`
очищает примечание. При замене состава заказа существующие позиции сохраняют
цену покупки, новые получают текущую цену каталога.

## Валидация и ошибки

- Обязательные строки непустые после удаления пробелов по краям, максимум
  255 символов; запрещены управляющие символы, кроме `\n`, `\r`, `\t`.
- Артикул — непустая уникальная строка до 255 символов по правилам `cps-core`.
- Цена — `0..99999999.99`, не более двух знаков после запятой.
  Категория обязательна при создании/изменении товара и должна существовать.
- Позиции заказа — непустой список без `null`, количество `1..100000`.
  Сервис также проверяет суммарное количество повторяющихся товаров.
- Складской остаток — целое `0..2147483647`.
- Обязательные `userId`, `managerId`, `actingUserId`, UUID, enum, даты,
  диапазоны поиска и вложенные DTO проверяются до вызова сервисов.
- Дробное количество, числовые enum, неизвестные JSON-поля и некорректный
  JSON отклоняются.

Коды: `400` — формат/валидация, `404` — ресурс/связь не найдены,
`409` — нарушение правил существующего сервиса или ограничений БД,
`500` — внутренняя ошибка. Для `409` сохраняются и существующие ошибки
сервисов о неверной роли/владельце; отдельная HTTP-политика доступа не добавляется.

Единый формат ошибки, в том числе для экспорта:

```json
{"timestamp":"2026-11-05T12:00:00Z","status":400,"error":"Bad Request","message":"Некорректные параметры запроса.","path":"/api/orders","violations":[{"field":"userId","message":"must not be null"}]}
```

SQL и трассировки исключений клиенту не возвращаются.

## Проверка

```bash
./gradlew :cps-backend:test :cps-backend:bootJar
```

HTTP-тесты используют реальный Spring-контекст, core-сервисы, Hibernate
и миграции Flyway; по умолчанию — H2 в режиме PostgreSQL.
Для отдельной PostgreSQL:

```bash
CPS_TEST_DB_URL=jdbc:postgresql://localhost:5432/cps_api_test \
CPS_TEST_DB_USERNAME=postgres CPS_TEST_DB_PASSWORD=postgres \
./gradlew :cps-backend:test --rerun-tasks
```

Тесты создают и удаляют собственную случайную схему. Поиск и сортировка
в handlers работают над списками core-сервисов в памяти, без пагинации.
