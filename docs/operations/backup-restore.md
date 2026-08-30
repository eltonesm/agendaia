# Backup e Restauração

> Escrito em 2026-08-30, **durante** o primeiro ensaio de restauração — não
> antes dele. Os números e saídas abaixo são do que realmente aconteceu.
>
> Documento de contexto, mas com função operacional: precisa ser encontrável às
> duas da manhã por alguém com sono.

## Por que isto é obrigatório

Uma VPS, um Postgres em container, e dentro dele a agenda inteira de
estabelecimentos que pagam mensalidade. Perder esse volume é perder os clientes
e a reputação junto — não há como reconstruir a agenda de um barbeiro.

**Um backup que nunca foi restaurado não é backup.** É um arquivo sobre o qual
se tem uma crença.

## Gerar backup

```bash
./scripts/backup.sh
```

Gera `backups/agendaia-AAAAMMDD-HHMMSS.dump` no formato custom do Postgres
(`-Fc`): comprimido, e permite restauração seletiva e paralela. Remove
automaticamente os dumps com mais de 30 dias (`RETENCAO_DIAS`).

O script **falha se o dump sair vazio**. Sem essa checagem, um `pg_dump` que
morre no meio deixa um arquivo de zero byte e o redirecionamento faz parecer que
deu certo — a forma mais comum de descobrir que não há backup é na hora de
precisar dele.

## Ensaio de restauração

```bash
./scripts/restore.sh backups/agendaia-20260830-154935.dump
```

Restaura para um banco **descartável** (`agendaia_restore_test`), sem tocar no
banco real. Prova que o dump é válido sem arriscar nada.

Saída do ensaio de 2026-08-30:

```
Recriando agendaia_restore_test...
Restaurando...

Conferindo o que chegou:
   tabelas: 2
   migrations aplicadas: 1
   btree_gist: 1
```

E os dados semeados voltaram intactos:

```
   1: sobreviveu
   2: ao restore
   3: de 2026-08-30
```

Descartar o banco de ensaio depois:

```bash
docker compose exec -T postgres psql -U agendaia -d postgres \
  -c 'DROP DATABASE agendaia_restore_test;'
```

## Restauração de verdade

```bash
./scripts/restore.sh backups/agendaia-....dump --para agendaia
```

**O banco de destino é apagado e recriado.** O script exige que se digite o nome
do banco para confirmar — não há `--force`, de propósito.

Antes de restaurar em produção: **pare a aplicação**. Restaurar sob escrita
concorrente produz um estado que não é nem o antigo nem o novo.

## O que conferir depois de qualquer restauração

| conferência | por quê |
|---|---|
| `flyway_schema_history` tem as migrations | schema incompleto quebra o `ddl-auto: validate` no próximo start |
| extensão `btree_gist` presente | sem ela a exclusion constraint não existe, e **o overbooking volta a ser possível** |
| contagem de agendamentos bate com o esperado | prova que o dado veio, não só o schema |

A segunda linha é a mais importante e a menos óbvia: um banco restaurado sem
`btree_gist` sobe, aceita agendamentos, e silenciosamente permite marcar dois
clientes no mesmo horário.

## Pendente para produção

Nada disto existe ainda — depende da VPS:

- [ ] **Destino fora da VPS.** Backup na mesma máquina do banco não protege
      contra a perda da máquina, que é o cenário mais provável. Backblaze B2 ou
      equivalente compatível com S3 custa centavos no volume projetado.
- [ ] **Cron diário**, em horário de baixo movimento.
- [ ] **Alerta quando o backup falhar.** Backup que falha em silêncio é pior
      que nenhum: cria a crença sem o arquivo.
- [ ] **Ensaio periódico de restauração**, mensal. Sem repetição, esta página
      envelhece e vira ficção.

Ver `TODO-105` e `TODO-106` no [backlog](../../sdd/backlog.md).
