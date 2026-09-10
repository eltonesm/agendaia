package com.simboraagendar.scheduling.application.port.in;

import java.util.UUID;

/**
 * Consulta os detalhes do agendamento pelo link (US-1, confirmacao-e-cancelamento,
 * TODO-007) — lança {@code AppointmentNotFoundException} para id de outro
 * tenant ou inexistente.
 */
public interface AppointmentDetailsUseCase {

    AppointmentDetails handle(UUID appointmentId);
}
