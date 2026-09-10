package com.simboraagendar.scheduling.application.port.in;

import java.util.UUID;

/**
 * Cancela o agendamento pelo link (US-3, confirmacao-e-cancelamento,
 * TODO-007, glossário) — {@code SCHEDULED}/{@code CONFIRMED} → {@code
 * CANCELLED}, liberando o horário (BR-5). Sem efeito quando já está
 * {@code CANCELLED} ou o horário já passou (BR-3/BR-4).
 */
public interface CancelAppointmentUseCase {

    void cancel(UUID appointmentId);
}
