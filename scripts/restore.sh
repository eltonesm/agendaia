#!/usr/bin/env bash
#
# Restauração do banco do AgendaIA.
#
#   ./scripts/restore.sh backups/agendaia-20260830-153000.dump              # ensaio
#   ./scripts/restore.sh backups/agendaia-....dump --para agendaia          # de verdade
#
# Por padrão restaura para um banco DESCARTÁVEL (agendaia_restore_test), sem
# tocar no banco real. É assim que se faz o ensaio de restauração: prova que o
# dump é válido sem arriscar o que está em produção.
#
# Use --para <banco> para restaurar de verdade. O banco de destino é APAGADO e
# recriado.

set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "uso: $0 <arquivo.dump> [--para <banco>]" >&2
  exit 1
fi

ARQUIVO="$1"; shift
SERVICO="${POSTGRES_SERVICE:-postgres}"
USUARIO="${POSTGRES_USER:-agendaia}"
DESTINO="agendaia_restore_test"
ENSAIO=true

while [[ $# -gt 0 ]]; do
  case "$1" in
    --para) DESTINO="$2"; ENSAIO=false; shift 2 ;;
    *) echo "opção desconhecida: $1" >&2; exit 1 ;;
  esac
done

[[ -s "$ARQUIVO" ]] || { echo "ERRO: $ARQUIVO não existe ou está vazio." >&2; exit 1; }

if [[ "$ENSAIO" == false ]]; then
  echo "ATENÇÃO: o banco '${DESTINO}' será APAGADO e recriado a partir de ${ARQUIVO}."
  read -r -p "Digite o nome do banco para confirmar: " confirmacao
  [[ "$confirmacao" == "$DESTINO" ]] || { echo "Cancelado."; exit 1; }
fi

psql_admin() {
  docker compose exec -T "$SERVICO" psql --username="$USUARIO" --dbname=postgres -v ON_ERROR_STOP=1 "$@"
}

echo "Recriando ${DESTINO}..."
psql_admin -c "DROP DATABASE IF EXISTS ${DESTINO} WITH (FORCE);" >/dev/null
psql_admin -c "CREATE DATABASE ${DESTINO};" >/dev/null

echo "Restaurando..."
docker compose exec -T "$SERVICO" \
  pg_restore --username="$USUARIO" --dbname="$DESTINO" --no-owner --exit-on-error \
  < "$ARQUIVO"

echo
echo "Conferindo o que chegou:"
docker compose exec -T "$SERVICO" psql --username="$USUARIO" --dbname="$DESTINO" -t -c "
  SELECT '  tabelas: ' || count(*) FROM information_schema.tables WHERE table_schema = 'public';"
docker compose exec -T "$SERVICO" psql --username="$USUARIO" --dbname="$DESTINO" -t -c "
  SELECT '  migrations aplicadas: ' || count(*) FROM flyway_schema_history WHERE success;" 2>/dev/null \
  || echo "  (sem flyway_schema_history — o dump não veio de um banco migrado)"
docker compose exec -T "$SERVICO" psql --username="$USUARIO" --dbname="$DESTINO" -t -c "
  SELECT '  btree_gist: ' || count(*) FROM pg_extension WHERE extname = 'btree_gist';"

echo
if [[ "$ENSAIO" == true ]]; then
  echo "Ensaio concluído. Para descartar:"
  echo "  docker compose exec -T ${SERVICO} psql -U ${USUARIO} -d postgres -c 'DROP DATABASE ${DESTINO};'"
else
  echo "Restauração concluída em '${DESTINO}'."
fi
