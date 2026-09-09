package com.agendaia.scheduling.application.port.in;

import java.util.UUID;

/**
 * Marca um agendamento como concluído, pelo painel do dono (US-4,
 * sistema-de-design-admin, TODO-110) — o atendimento aconteceu de verdade,
 * não só "confirmado". Só transiciona a partir de {@code SCHEDULED}/
 * {@code CONFIRMED}, e só depois que {@code startsAt} já chegou (BR-1/BR-3).
 */
public interface CompleteAppointmentUseCase {

    void complete(UUID appointmentId);
}
