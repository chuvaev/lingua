# Lingua

Система изучения иностранного языка с локальной LLM.

- **backend/** — Java 27, Spring Boot 4.1 (WebFlux), Maven multi-module
  - `contracts` — события Kafka и общие DTO
  - `learning-core` — колоды слов, повторение, прогресс (R2DBC, схема `learning`)
  - `ai-orchestrator` — диалог с репетитором: Spring AI + Ollama + tools, стрим через SSE
  - `content-rag` — учебные материалы, эмбеддинги, поиск по pgvector (схема `content`)
- **frontend/** — React + TypeScript + Vite
- **deploy/** — Docker Compose, скрипт запуска, пример `.env`

Инфраструктура: PostgreSQL + pgvector, Kafka (KRaft), Redis. Ollama работает на отдельной машине с GPU.

## Первый запуск (разработка)

Нужны: JDK 27, Maven 3.9+, Node.js 22+, Docker.

```bash
# 1. Инфраструктура: Postgres, Redis, Kafka
cd deploy && ./deploy.sh dev

# 2. Maven wrapper (один раз), затем сборка
cd ../backend
mvn -N wrapper:wrapper
./mvnw verify

# 3. Сервисы — из IDEA (Active profiles: dev) или из терминала:
./mvnw -pl learning-core spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Фронт
cd ../frontend && npm install && npm run dev   # http://localhost:5173
```

Значения по умолчанию в `application.yml` смотрят на dev-инфраструктуру,
поэтому при запуске из IDEA ничего настраивать не нужно.

## Ollama

На машине с GPU:

```bash
ollama pull qwen3:8b
ollama pull bge-m3
```

Адрес задаётся переменной `OLLAMA_BASE_URL` (по умолчанию `http://localhost:11434`).
Порт 11434 открывай в брандмауэре только для IP сервера.

## Проверка API

```bash
# Добавить слово
curl -X POST localhost:8081/api/learning/cards \
  -H 'X-User-Id: ilya' -H 'Content-Type: application/json' \
  -d '{"word":"apple","translation":"яблоко","language":"en"}'

# Чат с репетитором (стрим)
curl -N -X POST localhost:8082/api/ai/chat \
  -H 'X-User-Id: ilya' -H 'Content-Type: application/json' \
  -d '{"message":"Yesterday I go to the cinema"}'

# Загрузить материал и поискать по смыслу
curl -X POST localhost:8083/internal/materials -H 'Content-Type: application/json' \
  -d '{"title":"Past Simple","language":"en","text":"We use Past Simple for finished actions..."}'
curl 'localhost:8083/internal/materials/search?q=finished+actions+in+the+past'
```

## Прод

```bash
# Образы бэкенда (без Docker daemon), тег — например, номер сборки
./mvnw -Dimage.registry=localhost:5000 -Dimage.tag=1 package jib:build

# Образ фронта
docker build -t localhost:5000/lingua/frontend:1 frontend && docker push localhost:5000/lingua/frontend:1

# На сервере: файлы из deploy/ лежат в /opt/lingua, .env заполнен
TAG=1 ./deploy.sh up
```

`deploy.sh` трогает только проекты `lingua` и `lingua-dev` и не затрагивает
остальные контейнеры на сервере.
