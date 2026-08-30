-- Extensões exigidas pelo domínio.
--
-- btree_gist permite combinar operadores de igualdade (tenant_id, professional_id)
-- com operadores de sobreposição de intervalo (tstzrange) na mesma exclusion
-- constraint. Sem ela, a barreira contra overbooking do ADR 0005 não pode ser
-- criada.
--
-- Verificado em 2026-08-28 contra postgres:18-alpine: btree_gist 1.8 disponível.

CREATE EXTENSION IF NOT EXISTS btree_gist;
