package com.agendaia.billing.application;

import com.agendaia.billing.domain.AccessStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Projeção para o painel do operador (US-2). */
public record EstablishmentView(
        UUID tenantId, String name, String slug, Instant createdAt, AccessStatus status, LocalDate accessValidUntil) {}
