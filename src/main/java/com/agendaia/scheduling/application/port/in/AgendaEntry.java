package com.agendaia.scheduling.application.port.in;

import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha da agenda do dono, com {@code canConfirm}/{@code canCancel}/
 * {@code canReschedule}/{@code canComplete} já calculados pelo handler,
 * nunca pelo controller nem pelo template (PATTERNS.md) — mesmo raciocínio
 * de {@link AppointmentDetails} (confirmacao-e-cancelamento, TODO-007),
 * agora para a visão do dono (agenda-profissional, TODO-008;
 * {@code canComplete}, sistema-de-design-admin, TODO-110). {@code
 * paymentStatus} não tem {@code canX} correspondente — qualquer valor pode
 * mudar para qualquer outro a qualquer momento (gestao-de-clientes, BR-2).
 */
public record AgendaEntry(
        UUID id,
        String customerName,
        String customerPhone,
        String serviceName,
        Instant startsAt,
        Instant endsAt,
        AppointmentStatus status,
        PaymentStatus paymentStatus,
        boolean canConfirm,
        boolean canCancel,
        boolean canReschedule,
        boolean canComplete) {}
