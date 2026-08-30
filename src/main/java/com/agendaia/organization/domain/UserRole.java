package com.agendaia.organization.domain;

/**
 * Papel do usuário no estabelecimento.
 *
 * <p>Um valor só, por enquanto — a restrição {@code app_user_role_valid} no
 * banco espelha isso. Quando aparecer o segundo papel, a migration que o
 * introduzir altera a restrição, e assim ela continua documentando o que existe
 * de fato.
 *
 * <p>Papéis distintos com permissões próprias são o gatilho para extrair um
 * contexto {@code iam} (IDEA-011).
 */
public enum UserRole {
    /** Dono do estabelecimento. Único papel do MVP. */
    OWNER
}
