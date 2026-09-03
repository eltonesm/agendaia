package com.agendaia.organization.application.command;

import java.time.Instant;
import java.util.UUID;

/**
 * Dados do cadastro de um bloqueio.
 *
 * <p>Nenhum campo de tenant — quem determina o estabelecimento é a sessão, no
 * {@code TenantContext}, nunca o que chega no formulário (DD-1).
 * {@code professionalId} nulo vale para o estabelecimento inteiro (DD-3).
 */
public record RegisterTimeOffCommand(UUID professionalId, Instant startsAt, Instant endsAt, String reason) {}
