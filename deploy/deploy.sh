#!/usr/bin/env bash
# Управление стеком lingua. Трогает только свои проекты: "lingua" и "lingua-dev".
#
# Полный стек:
#   ./deploy.sh up [--with-grammar]   поднять / обновить
#   ./deploy.sh down                  остановить (данные сохраняются)
#   ./deploy.sh status | logs [svc]
#
# Разработка (только Postgres, Redis, Kafka; сервисы — из IDEA):
#   ./deploy.sh dev                   поднять dev-инфраструктуру
#   ./deploy.sh dev-down              остановить её
#   ./deploy.sh dev-logs [svc]
set -euo pipefail
cd "$(dirname "$0")"

PROD=(docker compose -p lingua -f compose.yaml)
DEV=(docker compose -p lingua-dev -f compose.dev.yaml)

load_env() {
  [[ -f .env ]] || { echo "Нет .env — скопируй .env.example и заполни"; exit 1; }
  local tag_override="${TAG:-}"           # TAG=42 ./deploy.sh up — тег из Jenkins важнее .env
  set -a; source .env; set +a
  [[ -n "$tag_override" ]] && TAG="$tag_override"
  export TAG
}

is_running() { [[ -n "$(docker compose -p "$1" ps -q 2>/dev/null)" ]]; }

cmd="${1:-up}"; shift || true

case "$cmd" in
  up)
    load_env
    [[ "${1:-}" == "--with-grammar" ]] && export COMPOSE_PROFILES=grammar

    if is_running lingua-dev; then
      echo "Запущена dev-инфраструктура. Останови её (./deploy.sh dev-down), чтобы не делить память."
      exit 1
    fi

    docker network inspect "$TRAEFIK_NETWORK" >/dev/null 2>&1 \
      || { echo "Сеть Traefik '$TRAEFIK_NETWORK' не найдена (docker network ls)"; exit 1; }

    if ! curl -fs --max-time 3 "$OLLAMA_BASE_URL/api/tags" >/dev/null; then
      echo "Внимание: Ollama ($OLLAMA_BASE_URL) недоступна — AI-функции будут ждать в очереди"
    fi

    "${PROD[@]}" pull --quiet
    "${PROD[@]}" up -d --remove-orphans --wait --wait-timeout 300
    "${PROD[@]}" ps
    ;;
  down)   load_env; "${PROD[@]}" down ;;          # без -v: тома с данными остаются
  status) load_env; "${PROD[@]}" ps ;;
  logs)   load_env; "${PROD[@]}" logs -f --tail=200 "$@" ;;

  dev)
    if is_running lingua; then
      echo "Внимание: полный стек тоже запущен. Для экономии памяти: ./deploy.sh down"
    fi
    "${DEV[@]}" up -d --wait --wait-timeout 180
    cat <<'EOF'

Dev-инфраструктура готова:
  PostgreSQL  jdbc:postgresql://localhost:15432/lingua  (lingua / lingua)
              r2dbc:postgresql://localhost:15432/lingua
  Redis       localhost:16379
  Kafka       localhost:19092
Сервисы из IDEA подключаются к ней без настроек (профиль dev — для подробных логов):
  learning-core :8081   ai-orchestrator :8082   content-rag :8083
Фронт: cd frontend && npm run dev  →  http://localhost:5173
EOF
    ;;
  dev-down) "${DEV[@]}" down ;;
  dev-logs) "${DEV[@]}" logs -f --tail=200 "$@" ;;

  *) echo "Неизвестная команда: $cmd (см. начало скрипта)"; exit 1 ;;
esac
