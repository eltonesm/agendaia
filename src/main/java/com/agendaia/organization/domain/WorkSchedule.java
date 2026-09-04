package com.agendaia.organization.domain;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.TimeRange;
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
 * Jornada recorrente semanal do profissional, em faixas — dado declarado,
 * não calculado (glossário). Raiz de agregado.
 *
 * <p>Mesmo regime CRUD de {@code Professional} (ADR 0002). Sem setter
 * público; criação por {@link #register}.
 *
 * <p><strong>Almoço recorrente é modelado como duas faixas no mesmo dia</strong>,
 * não como {@code TimeOff} — o vão entre elas É o almoço. A checagem de
 * sobreposição (BR-3) é responsabilidade do caso de uso, não desta classe:
 * uma faixa isolada não sabe das outras faixas do mesmo profissional.
 */
@Entity
@Table(name = "work_schedule")
public class WorkSchedule {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 9)
    private DayOfWeek dayOfWeek;

    @Column(name = "starts_at", nullable = false)
    private LocalTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalTime endsAt;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected WorkSchedule() {
        // JPA
    }

    private WorkSchedule(
            UUID id,
            UUID tenantId,
            UUID professionalId,
            DayOfWeek dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            Instant agora) {
        this.id = id;
        this.tenantId = tenantId;
        this.professionalId = professionalId;
        this.dayOfWeek = dayOfWeek;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.active = true;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /** Declara uma faixa de jornada de um profissional do estabelecimento da sessão. */
    public static WorkSchedule register(
            TenantId tenantId, UUID professionalId, DayOfWeek dayOfWeek, LocalTime startsAt, LocalTime endsAt) {
        return register(tenantId, professionalId, dayOfWeek, startsAt, endsAt, Instant.now());
    }

    static WorkSchedule register(
            TenantId tenantId,
            UUID professionalId,
            DayOfWeek dayOfWeek,
            LocalTime startsAt,
            LocalTime endsAt,
            Instant agora) {
        if (tenantId == null) {
            throw new IllegalArgumentException("jornada não existe sem estabelecimento");
        }
        if (professionalId == null) {
            throw new IllegalArgumentException("jornada não existe sem profissional");
        }
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("jornada não existe sem dia da semana");
        }
        if (startsAt == null || endsAt == null) {
            throw new IllegalArgumentException("jornada precisa de início e fim");
        }
        if (!endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("fim deve ser depois do início");
        }
        return new WorkSchedule(UuidV7.generate(), tenantId.value(), professionalId, dayOfWeek, startsAt, endsAt, agora);
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

    public UUID professionalId() {
        return professionalId;
    }

    public DayOfWeek dayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime startsAt() {
        return startsAt;
    }

    public LocalTime endsAt() {
        return endsAt;
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

    /**
     * Sobreposição no tempo com outra faixa, sentido meio-aberto {@code [)} —
     * faixas encostadas (fim de uma igual ao início da outra) não se
     * sobrepõem. É o mecanismo do intervalo de almoço.
     *
     * <p>Delega a {@link TimeRange} (consultar-horarios-disponiveis, TODO-005)
     * — mesma fórmula que já estava aqui, agora reutilizável.
     */
    public boolean overlaps(WorkSchedule outra) {
        return new TimeRange(this.startsAt, this.endsAt).overlaps(new TimeRange(outra.startsAt, outra.endsAt));
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof WorkSchedule faixa && Objects.equals(id, faixa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "WorkSchedule[id=%s, tenantId=%s, professionalId=%s, dayOfWeek=%s, startsAt=%s, endsAt=%s]"
                .formatted(id, tenantId, professionalId, dayOfWeek, startsAt, endsAt);
    }
}
