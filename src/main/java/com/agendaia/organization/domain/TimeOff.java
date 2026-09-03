package com.agendaia.organization.domain;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Indisponibilidade <strong>excepcional e datada</strong> — diferente de
 * {@link WorkSchedule}, que é recorrente (glossário).
 *
 * <p>Mesmo regime CRUD de {@code Professional} (ADR 0002). Sem setter
 * público; criação por {@link #register}.
 *
 * <p><strong>{@code professionalId} é anulável de propósito</strong> (DD-3):
 * nulo significa que o bloqueio vale para o estabelecimento inteiro — é
 * assim que feriado e fechamento para reforma são representados, sem tabela
 * nem coluna de tipo separada.
 */
@Entity
@Table(name = "time_off")
public class TimeOff {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "professional_id")
    private UUID professionalId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected TimeOff() {
        // JPA
    }

    private TimeOff(
            UUID id, UUID tenantId, UUID professionalId, Instant startsAt, Instant endsAt, String reason, Instant agora) {
        this.id = id;
        this.tenantId = tenantId;
        this.professionalId = professionalId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.reason = reason;
        this.active = true;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /**
     * Registra um bloqueio do estabelecimento da sessão. {@code professionalId}
     * nulo vale para o estabelecimento inteiro (DD-3); {@code reason} é
     * opcional.
     */
    public static TimeOff register(TenantId tenantId, UUID professionalId, Instant startsAt, Instant endsAt, String reason) {
        return register(tenantId, professionalId, startsAt, endsAt, reason, Instant.now());
    }

    static TimeOff register(
            TenantId tenantId, UUID professionalId, Instant startsAt, Instant endsAt, String reason, Instant agora) {
        if (tenantId == null) {
            throw new IllegalArgumentException("bloqueio não existe sem estabelecimento");
        }
        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException("bloqueio precisa de início e fim");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("fim deve ser depois do início");
        }
        var motivoLimpo = reason == null ? null : reason.strip();
        if (motivoLimpo != null && motivoLimpo.isEmpty()) {
            motivoLimpo = null;
        }
        return new TimeOff(UuidV7.generate(), tenantId.value(), professionalId, startsAt, endsAt, motivoLimpo, agora);
    }

    /** Retira de circulação sem apagar (ADR 0011). Sem tela nesta feature. */
    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public TenantId tenantId() {
        return TenantId.of(tenantId);
    }

    /** {@code null} significa que o bloqueio vale para o estabelecimento inteiro (DD-3). */
    public UUID professionalId() {
        return professionalId;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant endsAt() {
        return endsAt;
    }

    public String reason() {
        return reason;
    }

    public boolean isActive() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof TimeOff bloqueio && Objects.equals(id, bloqueio.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "TimeOff[id=%s, tenantId=%s, professionalId=%s, startsAt=%s, endsAt=%s]"
                .formatted(id, tenantId, professionalId, startsAt, endsAt);
    }
}
