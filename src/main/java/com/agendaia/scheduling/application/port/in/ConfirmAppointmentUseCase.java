package com.agendaia.scheduling.application.port.in;

import java.util.UUID;

/**
 * Confirma presença no agendamento pelo link (US-2, confirmacao-e-cancelamento,
 * TODO-007) — {@code SCHEDULED} → {@code CONFIRMED}. Sem efeito quando o
 * agendamento já está {@code CONFIRMED}/{@code CANCELLED} ou o horário já
 * passou (BR-2/BR-3/BR-4).
 */
public interface ConfirmAppointmentUseCase {

    void confirm(UUID appointmentId);
}
