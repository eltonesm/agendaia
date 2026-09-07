package com.agendaia.scheduling.application.port.in;

import com.agendaia.scheduling.domain.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha da agenda do dono, com {@code canConfirm}/{@code canCancel}/
 * {@code canReschedule} já calculados pelo handler, nunca pelo controller
 * nem pelo template (PATTERNS.md) — mesmo raciocínio de
 * {@link AppointmentDetails} (confirmacao-e-cancelamento, TODO-007), agora
 * para a visão do dono (agenda-profissional, TODO-008).
 */
public record AgendaEntry(
        UUID id,
        String customerName,
        String customerPhone,
        String serviceName,
        Instant startsAt,
        Instant endsAt,
        AppointmentStatus status,
        boolean canConfirm,
        boolean canCancel,
        boolean canReschedule) {}
