package com.simboraagendar.scheduling.application;

import com.simboraagendar.catalog.api.ServiceOfferingRef;
import com.simboraagendar.scheduling.application.port.out.AppointmentRepository;
import com.simboraagendar.scheduling.domain.Appointment;
import com.simboraagendar.shared.TenantId;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Monta o retrato do {@link Appointment} (BR-2) e grava — lógica repetida
 * por {@code BookAppointmentHandler} (reserva pública, TODO-006) e por
 * {@code ProfessionalAgendaHandler} (criação e reagendamento manual pelo
 * dono, agenda-profissional, TODO-008, DD-5). Extraída só depois de existir
 * um terceiro chamador real (reagendar cria antes de cancelar, DD-9) —
 * pacote-privada, nenhum outro contexto precisa dela.
 */
final class AppointmentFactory {

    private AppointmentFactory() {
        // utilitário
    }

    static Appointment buildAndSave(
            AppointmentRepository appointmentRepository,
            TenantId tenantId,
            UUID professionalId,
            UUID customerId,
            ServiceOfferingRef oferta,
            Instant startsAt) {
        var endsAt = startsAt.plus(oferta.durationMinutes(), ChronoUnit.MINUTES);

        var agendamento = Appointment.schedule(
                tenantId,
                professionalId,
                oferta.id(),
                customerId,
                oferta.serviceName(),
                oferta.durationMinutes(),
                oferta.price(),
                startsAt,
                endsAt);

        return appointmentRepository.save(agendamento);
    }
}
