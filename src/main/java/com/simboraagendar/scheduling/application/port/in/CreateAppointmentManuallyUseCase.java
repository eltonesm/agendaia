package com.simboraagendar.scheduling.application.port.in;

/**
 * Cria um agendamento diretamente pelo painel do dono (US-2, agenda-
 * profissional, TODO-008) — mesmo {@link BookAppointmentCommand} da reserva
 * pública (TODO-006): o profissional é derivado da oferta escolhida, igual
 * ao fluxo do cliente. Diferença de comportamento (BR-4: sem teto de 3
 * agendamentos futuros por telefone) fica na implementação, não na
 * assinatura.
 */
public interface CreateAppointmentManuallyUseCase {

    BookedAppointment create(BookAppointmentCommand command);
}
