package com.agendaia.organization.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Projeção de {@code Business} exportada para outros contextos.
 *
 * <p>Só o mínimo necessário para o painel do operador (TODO-009) — nenhum
 * dado de {@code User} (e-mail, senha), nenhum dado sensível.
 */
public record BusinessRef(UUID tenantId, String name, String slug, Instant createdAt) {}
