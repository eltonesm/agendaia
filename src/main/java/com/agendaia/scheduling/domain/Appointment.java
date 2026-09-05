package com.agendaia.scheduling.domain;

import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Um atendimento marcado (glossário). Guarda o <strong>retrato</strong> de
 * duração e preço no momento da reserva (BR-2 da spec funcional de
 * pagina-publica-agendamento) — mudança futura no catálogo não afeta
 * agendamentos já criados.
 *
 * <p>Java puro, regime completo (ADR 0002) — {@code scheduling} é o core
 * domain. A garantia real contra overbooking é a exclusion constraint do
 * banco (ADR 0005); {@link #schedule} só valida forma, nunca sobreposição
 * de horário.
 */
public final class Appointment {

    private final UUID id;
    private final UUID tenantId;
    private final UUID professionalId;
    private final UUID serviceOfferingId;
    private final UUID customerId;
    private final AppointmentStatus status;
    private final Instant startsAt;
    private final Instant endsAt;
    private final String serviceName;
    private final int durationMinutes;
    private final Money price;

    private Appointment(
            UUID id,
            UUID tenantId,
            UUID professionalId,
            UUID serviceOfferingId,
            UUID customerId,
            AppointmentStatus status,
            Instant startsAt,
            Instant endsAt,
            String serviceName,
            int durationMinutes,
            Money price) {
        this.id = id;
        this.tenantId = tenantId;
        this.professionalId = professionalId;
        this.serviceOfferingId = serviceOfferingId;
        this.customerId = customerId;
        this.status = status;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.serviceName = serviceName;
        this.durationMinutes = durationMinutes;
        this.price = price;
    }

    /** Nasce sempre {@link AppointmentStatus#SCHEDULED} (BR-1). */
    public static Appointment schedule(
            TenantId tenantId,
            UUID professionalId,
            UUID serviceOfferingId,
            UUID customerId,
            String serviceName,
            int durationMinutes,
            Money price,
            Instant startsAt,
            Instant endsAt) {
        if (tenantId == null) {
            throw new IllegalArgumentException("agendamento não existe sem estabelecimento");
        }
        if (professionalId == null || serviceOfferingId == null || customerId == null) {
            throw new IllegalArgumentException("agendamento precisa de profissional, oferta e cliente");
        }
        var nomeLimpo = serviceName == null ? "" : serviceName.strip();
        if (nomeLimpo.isEmpty()) {
            throw new IllegalArgumentException("agendamento precisa do nome do serviço no momento da reserva");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("duração do agendamento precisa ser positiva");
        }
        if (price == null) {
            throw new IllegalArgumentException("agendamento precisa de um preço, mesmo que zero");
        }
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("agendamento precisa de início e fim válidos");
        }
        return new Appointment(
                UuidV7.generate(),
                tenantId.value(),
                professionalId,
                serviceOfferingId,
                customerId,
                AppointmentStatus.SCHEDULED,
                startsAt,
                endsAt,
                nomeLimpo,
                durationMinutes,
                price);
    }

    /** Reconstrói a partir de dado persistido — usado só pelo mapper (adapter.out.persistence). */
    public static Appointment reconstitute(
            UUID id,
            TenantId tenantId,
            UUID professionalId,
            UUID serviceOfferingId,
            UUID customerId,
            AppointmentStatus status,
            Instant startsAt,
            Instant endsAt,
            String serviceName,
            int durationMinutes,
            Money price) {
        return new Appointment(
                id,
                tenantId.value(),
                professionalId,
                serviceOfferingId,
                customerId,
                status,
                startsAt,
                endsAt,
                serviceName,
                durationMinutes,
                price);
    }

    public UUID id() {
        return id;
    }

    public TenantId tenantId() {
        return TenantId.of(tenantId);
    }

    public UUID professionalId() {
        return professionalId;
    }

    public UUID serviceOfferingId() {
        return serviceOfferingId;
    }

    public UUID customerId() {
        return customerId;
    }

    public AppointmentStatus status() {
        return status;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant endsAt() {
        return endsAt;
    }

    public String serviceName() {
        return serviceName;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public Money price() {
        return price;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Appointment appointment && Objects.equals(id, appointment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Sem dado de cliente (nome/telefone vivem em customer, nem aqui de qualquer forma). */
    @Override
    public String toString() {
        return "Appointment[id=%s, tenantId=%s, status=%s, startsAt=%s]"
                .formatted(id, tenantId, status, startsAt);
    }
}
