package com.agendaia.organization.application.port.in;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha da lista de bloqueios. {@code professionalName} é {@code null}
 * quando o bloqueio vale para o estabelecimento inteiro (DD-3) — o template
 * decide o texto de exibição.
 */
public record TimeOffView(UUID id, String professionalName, Instant startsAt, Instant endsAt, String reason) {}
