# EXPLAIN.md — Анализ поискового запроса

## Поисковый запрос

Поиск документов по статусу + автору + периоду (по дате создания):

```
GET /api/documents/search?status=DRAFT&author=Иванов&dateFrom=2025-01-01T00:00:00&dateTo=2025-06-30T23:59:59&page=0&size=20&sort=createdAt,desc
```

## SQL-запрос (генерируемый JPA)

```sql
SELECT d.id, d.document_number, d.author, d.title, d.status, d.version, d.created_at, d.updated_at
FROM document d
WHERE d.status = 'DRAFT'
  AND LOWER(d.author) LIKE '%иванов%'
  AND d.created_at >= '2025-01-01 00:00:00'
  AND d.created_at <= '2025-06-30 23:59:59'
ORDER BY d.created_at DESC
LIMIT 20 OFFSET 0;
```

## EXPLAIN ANALYZE (ожидаемый результат)

```
Limit  (cost=0.42..12.50 rows=20 width=120) (actual time=0.045..0.120 rows=20 loops=1)
  ->  Index Scan Backward using idx_document_status_created_at on document d
        (cost=0.42..580.00 rows=950 width=120) (actual time=0.043..0.110 rows=20 loops=1)
        Index Cond: ((status = 'DRAFT') AND (created_at >= '2025-01-01' AND created_at <= '2025-06-30'))
        Filter: (lower(author) ~~ '%иванов%')
        Rows Removed by Filter: 5
Planning Time: 0.150 ms
Execution Time: 0.180 ms
```

## Используемые индексы

| Индекс | Столбцы | Назначение |
|--------|---------|------------|
| `idx_document_status` | `status` | Фильтрация по статусу |
| `idx_document_created_at` | `created_at` | Фильтрация по периоду, сортировка |
| `idx_document_status_created_at` | `status, created_at` | **Составной индекс** — покрывает одновременно фильтр по статусу и диапазон по дате. Наиболее эффективен для данного запроса. |
| `idx_document_author` | `author` | Фильтрация по автору (если LIKE без ведущего %) |

## Пояснение

1. **Составной индекс `(status, created_at)`** — основной для данного запроса. PostgreSQL использует его для:
   - Быстрого нахождения записей с нужным статусом.
   - Диапазонного сканирования по `created_at` внутри этого статуса.
   - Обратного сканирования для `ORDER BY created_at DESC`.

2. **Фильтр по автору** (`LIKE '%иванов%'`) применяется как post-filter после индексного сканирования, так как ведущий `%` не позволяет использовать B-tree индекс. Для полнотекстового поиска по автору можно добавить GIN/GiST индекс с `pg_trgm`.

3. **LIMIT + OFFSET** — PostgreSQL прекращает сканирование, как только набрал нужное количество строк (20), что делает запрос эффективным даже на больших таблицах.

## Рекомендации

- При большом объёме данных и частых поисках по автору с `LIKE '%...%'` — добавить расширение `pg_trgm` и GIN-индекс:
  ```sql
  CREATE EXTENSION IF NOT EXISTS pg_trgm;
  CREATE INDEX idx_document_author_trgm ON document USING gin (author gin_trgm_ops);
  ```
- Для cursor-based пагинации (вместо OFFSET) использовать `WHERE created_at < :lastSeen ORDER BY created_at DESC LIMIT 20` — избегает деградации на больших OFFSET.
