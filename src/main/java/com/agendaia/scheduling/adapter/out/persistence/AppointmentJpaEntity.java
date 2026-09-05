package com.agendaia.scheduling.adapter.out.persistence;

import com.agendaia.scheduling.domain.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Espelho de persistência de {@code Appointment} (regime completo, ADR
 * 0002) — {@code scheduling.domain} não conhece JPA. Conversão nos dois
 * sentidos em {@link AppointmentMapper}.
 *
 * <p>A garantia real contra overbooking está na exclusion constraint da
 * migration {@code V8}, não em nenhum campo desta classe.
 */
@Entity
@Table(name = "appointment")
public class AppointmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "service_offering_id", nullable = false)
    private UUID serviceOfferingId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "service_name", nullable = false, length = 120)
    private String serviceName;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected AppointmentJpaEntity() {
        // JPA
    }

    AppointmentJpaEntity(
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
            long priceCents,
            Instant createdAt,
            Instant updatedAt) {
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
        this.priceCents = priceCents;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    UUID id() {
        return id;
    }

    UUID tenantId() {
        return tenantId;
    }

    UUID professionalId() {
        return professionalId;
    }

    UUID serviceOfferingId() {
        return serviceOfferingId;
    }

    UUID customerId() {
        return customerId;
    }

    AppointmentStatus status() {
        return status;
    }

    Instant startsAt() {
        return startsAt;
    }

    Instant endsAt() {
        return endsAt;
    }

    String serviceName() {
        return serviceName;
    }

    int durationMinutes() {
        return durationMinutes;
    }

    long priceCents() {
        return priceCents;
    }
}
