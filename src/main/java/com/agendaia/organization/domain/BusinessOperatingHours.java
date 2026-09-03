package com.agendaia.organization.domain;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Quando o estabelecimento <strong>pode</strong> abrir — limite externo da
 * disponibilidade (glossário). Entidade de {@code Business}, sem identidade
 * própria fora dele.
 *
 * <p>Mesmo regime CRUD de {@code Business}/{@code Professional} (ADR 0002):
 * a entidade JPA é o modelo, sem classe espelho nem mapper. Sem setter
 * público; criação por {@link #register}.
 *
 * <p>Várias faixas por dia são permitidas; dia sem nenhuma faixa é dia
 * fechado — não existe um campo "fechado" separado.
 */
@Entity
@Table(name = "business_operating_hours")
public class BusinessOperatingHours {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(name = "opens_at", nullable = false)
    private LocalTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalTime closesAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected BusinessOperatingHours() {
        // JPA
    }

    private BusinessOperatingHours(
            UUID id, UUID tenantId, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt, Instant agora) {
        this.id = id;
        this.tenantId = tenantId;
        this.dayOfWeek = dayOfWeek;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
        this.active = true;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /** Declara uma faixa de funcionamento do estabelecimento da sessão. */
    public static BusinessOperatingHours register(
            TenantId tenantId, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        return register(tenantId, dayOfWeek, opensAt, closesAt, Instant.now());
    }

    static BusinessOperatingHours register(
            TenantId tenantId, DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt, Instant agora) {
        if (tenantId == null) {
            throw new IllegalArgumentException("horário de funcionamento não existe sem estabelecimento");
        }
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("horário de funcionamento não existe sem dia da semana");
        }
        if (opensAt == null || closesAt == null) {
            throw new IllegalArgumentException("horário de funcionamento precisa de abertura e fechamento");
        }
        if (!closesAt.isAfter(opensAt)) {
            throw new IllegalArgumentException("fechamento deve ser depois da abertura");
        }
        return new BusinessOperatingHours(UuidV7.generate(), tenantId.value(), dayOfWeek, opensAt, closesAt, agora);
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

    public DayOfWeek dayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime opensAt() {
        return opensAt;
    }

    public LocalTime closesAt() {
        return closesAt;
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
        return outro instanceof BusinessOperatingHours faixa && Objects.equals(id, faixa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "BusinessOperatingHours[id=%s, tenantId=%s, dayOfWeek=%s, opensAt=%s, closesAt=%s]"
                .formatted(id, tenantId, dayOfWeek, opensAt, closesAt);
    }
}
