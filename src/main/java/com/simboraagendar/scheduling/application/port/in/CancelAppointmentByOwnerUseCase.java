package com.simboraagendar.scheduling.application.port.in;

import java.util.UUID;

/**
 * Cancela um agendamento pelo painel do dono (US-3, agenda-profissional,
 * TODO-008). Interface distinta de {@link CancelAppointmentUseCase} apesar
 * da assinatura idêntica: mesma justificativa de {@link
 * CreateAppointmentManuallyUseCase} vs. {@code BookAppointmentUseCase} —
 * evita ambiguidade de bean do Spring entre duas implementações do mesmo
 * contrato, e mantém visível no tipo a diferença de regra (BR-2: sem
 * restrição de horário, ao contrário do cancelamento pelo cliente, BR-4 de
 * confirmacao-e-cancelamento).
 */
public interface CancelAppointmentByOwnerUseCase {

    void cancel(UUID appointmentId);
}
