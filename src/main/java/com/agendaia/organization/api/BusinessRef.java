package com.agendaia.organization.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Projeção de {@code Business} exportada para outros contextos.
 *
 * <p>Só o mínimo necessário para o painel do operador (TODO-009) e para a
 * página pública de agendamento — nenhum dado de {@code User} (e-mail,
 * senha), nenhum dado sensível.
 *
 * @param whatsapp nulo quando o estabelecimento não cadastrou (BR-8 de
 *     confirmacao-e-cancelamento, TODO-007)
 */
public record BusinessRef(UUID tenantId, String name, String slug, String whatsapp, Instant createdAt) {}
