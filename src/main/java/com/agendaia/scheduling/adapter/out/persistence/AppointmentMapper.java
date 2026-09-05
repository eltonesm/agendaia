package com.agendaia.scheduling.adapter.out.persistence;

import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import java.time.Instant;

/** Conversão entre {@link Appointment} (domínio puro) e {@link AppointmentJpaEntity} (regime completo, ADR 0002). */
final class AppointmentMapper {

    private AppointmentMapper() {
        // utilitário
    }

    /**
     * {@code createdAt}/{@code updatedAt} são metadado de persistência, não
     * do domínio (Appointment não tem os dois campos) — esta feature só faz
     * {@code INSERT}, nunca update, então os dois nascem iguais a agora.
     */
    static AppointmentJpaEntity toEntity(Appointment appointment) {
        var agora = Instant.now();
        return new AppointmentJpaEntity(
                appointment.id(),
                appointment.tenantId().value(),
                appointment.professionalId(),
                appointment.serviceOfferingId(),
                appointment.customerId(),
                appointment.status(),
                appointment.startsAt(),
                appointment.endsAt(),
                appointment.serviceName(),
                appointment.durationMinutes(),
                appointment.price().cents(),
                agora,
                agora);
    }

    static Appointment toDomain(AppointmentJpaEntity entity) {
        return Appointment.reconstitute(
                entity.id(),
                TenantId.of(entity.tenantId()),
                entity.professionalId(),
                entity.serviceOfferingId(),
                entity.customerId(),
                entity.status(),
                entity.startsAt(),
                entity.endsAt(),
                entity.serviceName(),
                entity.durationMinutes(),
                new Money(entity.priceCents()));
    }
}
