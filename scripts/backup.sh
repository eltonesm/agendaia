#!/usr/bin/env bash
#
# Backup do banco do AgendaIA.
#
#   ./scripts/backup.sh
#
# Gera um dump no formato custom do Postgres (-Fc), que é comprimido e permite
# restauração seletiva e paralela. Guarda em backups/ e remove os mais antigos
# que RETENCAO_DIAS.
#
# Um backup que nunca foi restaurado não é backup. Faça a rotina de restore
# (scripts/restore.sh) periodicamente — ver docs/operations/backup-restore.md.

set -euo pipefail

SERVICO="${POSTGRES_SERVICE:-postgres}"
BANCO="${POSTGRES_DB:-agendaia}"
USUARIO="${POSTGRES_USER:-agendaia}"
DESTINO="${BACKUP_DIR:-backups}"
RETENCAO_DIAS="${RETENCAO_DIAS:-30}"

carimbo="$(date +%Y%m%d-%H%M%S)"
arquivo="${DESTINO}/${BANCO}-${carimbo}.dump"

mkdir -p "$DESTINO"

echo "Gerando dump de ${BANCO}..."
docker compose exec -T "$SERVICO" \
  pg_dump --username="$USUARIO" --dbname="$BANCO" --format=custom --no-owner \
  > "$arquivo"

tamanho="$(du -h "$arquivo" | cut -f1)"
echo "OK  ${arquivo}  (${tamanho})"

# Um dump de zero byte é falha silenciosa: o pg_dump pode ter morrido e o
# redirecionamento criado o arquivo mesmo assim.
if [[ ! -s "$arquivo" ]]; then
  echo "ERRO: dump vazio — o backup falhou." >&2
  rm -f "$arquivo"
  exit 1
fi

removidos="$(find "$DESTINO" -name "${BANCO}-*.dump" -mtime "+${RETENCAO_DIAS}" -print -delete | wc -l)"
[[ "$removidos" -gt 0 ]] && echo "Removidos ${removidos} backup(s) com mais de ${RETENCAO_DIAS} dias."

echo
echo "Envie para fora da VPS. Backup na mesma máquina do banco não protege"
echo "contra a perda da máquina, que é o cenário mais provável."
