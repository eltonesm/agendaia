package com.simboraagendar.scheduling.application.port.in;

import com.simboraagendar.scheduling.domain.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Dados prontos para exibir na tela do agendamento (confirmacao-e-cancelamento,
 * TODO-007) — {@code canConfirm}/{@code canCancel} já calculados pelo
 * handler, nunca pelo controller nem pelo template (PATTERNS.md).
 */
public record AppointmentDetails(
        UUID id,
        String professionalName,
        String customerName,
        String serviceName,
        Instant startsAt,
        Instant endsAt,
        AppointmentStatus status,
        boolean canConfirm,
        boolean canCancel) {}
