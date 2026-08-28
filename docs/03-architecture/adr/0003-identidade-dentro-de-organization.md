# ADR 0003 — Identidade dentro de Organization, sem contexto IAM

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

A proposta inicial tinha um contexto `iam` responsável por usuário,
autenticação, papel **e tenant**, ao lado de um contexto `organization`
responsável pela empresa.

Neste produto, porém, o tenant **é** a empresa: não existe Barbearia do João
sem tenant, nem tenant sem Barbearia do João. Duas raízes de agregado para uma
entidade do mundo real criam um problema de sincronização desde o primeiro dia.

O sintoma que expôs o problema: o fluxo "empresa cria conta" grava a empresa e o
usuário dono na mesma transação. Isso é uma escrita atravessando dois
contextos — exatamente o que a arquitetura proíbe. Quando uma regra de
fronteira torna impossível um caso de uso trivial, a fronteira está no lugar
errado.

## Decisão

Nós **não vamos** criar um contexto `iam` no MVP. `Business` é a raiz de
agregado e é o tenant; `User` é uma entidade que pertence a ela. O cadastro
acontece dentro de uma transação, num contexto só.

O tipo `TenantId` continua existindo no `shared-kernel`, separado de
`BusinessId`, para que o isolamento não dependa de a coincidência 1:1 continuar
valendo.

## Consequências

O cadastro de empresa fica trivial e não viola nenhuma regra arquitetural.

`organization` fica sendo o maior módulo depois do core, acumulando empresa,
usuário, profissional, jornada e bloqueios. É uma concentração aceitável
enquanto todos esses conceitos giram em torno de "quem somos e quando
trabalhamos".

Autenticação e autorização ficam misturadas a cadastro dentro do mesmo módulo,
o que é menos elegante e mais prático.

## Gatilho de reavaliação

Extrair `iam` quando aparecer o primeiro destes: login para o cliente final,
múltiplos usuários por empresa com permissões distintas, SSO, convites, ou
auditoria de acesso. Nessa extração, `iam` fica com credencial e papel, e
referencia `tenantId` por id — sem trazer `Business` junto.
