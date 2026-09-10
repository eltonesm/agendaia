package com.simboraagendar.scheduling.application.port.in;

/**
 * Move um agendamento para outro horário (e, opcionalmente, outro
 * profissional) — US-4, agenda-profissional, TODO-008. Sujeito às mesmas
 * invariantes de uma reserva nova (glossário) — a garantia real contra
 * overbooking continua sendo a exclusion constraint do banco (ADR 0005).
 */
public interface RescheduleAppointmentUseCase {

    BookedAppointment reschedule(RescheduleAppointmentCommand command);
}
