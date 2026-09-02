package com.agendaia.catalog.domain;

import com.agendaia.shared.Money;
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
 * O que o cliente de fato agenda: um {@link Service} executado por um
 * profissional específico, com duração, preço e intervalo próprios.
 *
 * <p>Mesmo regime CRUD de {@code organization} (ADR 0002). {@code price} é
 * guardado como {@code long} bruto ({@code priceCents}) — mesmo padrão de
 * {@link TenantId} em {@code Professional}: campo interno primitivo, tipo
 * rico só na borda pública ({@link #price()}).
 *
 * <p>{@code professionalId} é UUID solto, sem FK: profissional é de
 * {@code organization}, outro contexto. A garantia de que ele pertence ao
 * tenant certo vem da aplicação, não do banco (DD-1/DD-2 da spec técnica).
 */
@Entity
@Table(name = "service_offering")
public class ServiceOffering {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "professional_id", nullable = false)
    private UUID professionalId;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected ServiceOffering() {
        // JPA
    }

    private ServiceOffering(
            UUID id,
            UUID tenantId,
            UUID serviceId,
            UUID professionalId,
            int durationMinutes,
            long priceCents,
            int bufferMinutes,
            Instant agora) {
        this.id = id;
        this.tenantId = tenantId;
        this.serviceId = serviceId;
        this.professionalId = professionalId;
        this.durationMinutes = durationMinutes;
        this.priceCents = priceCents;
        this.bufferMinutes = bufferMinutes;
        this.active = true;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /**
     * Cadastra uma oferta do estabelecimento da sessão. {@code duration}
     * não precisa ser múltiplo de 10 — a grade fixa de slot governa o
     * início do agendamento, não a duração do serviço (ADR 0006).
     */
    public static ServiceOffering register(
            TenantId tenantId,
            UUID serviceId,
            UUID professionalId,
            int durationMinutes,
            Money price,
            int bufferMinutes) {
        return register(tenantId, serviceId, professionalId, durationMinutes, price, bufferMinutes, Instant.now());
    }

    static ServiceOffering register(
            TenantId tenantId,
            UUID serviceId,
            UUID professionalId,
            int durationMinutes,
            Money price,
            int bufferMinutes,
            Instant agora) {
        if (tenantId == null) {
            throw new IllegalArgumentException("oferta não existe sem estabelecimento");
        }
        if (serviceId == null) {
            throw new IllegalArgumentException("oferta não existe sem serviço");
        }
        if (professionalId == null) {
            throw new IllegalArgumentException("oferta não existe sem profissional");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("duração da oferta deve ser maior que zero");
        }
        if (price == null) {
            throw new IllegalArgumentException("oferta não existe sem preço");
        }
        if (bufferMinutes < 0) {
            throw new IllegalArgumentException("intervalo da oferta não pode ser negativo");
        }
        return new ServiceOffering(
                UuidV7.generate(),
                tenantId.value(),
                serviceId,
                professionalId,
                durationMinutes,
                price.cents(),
                bufferMinutes,
                agora);
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

    public UUID serviceId() {
        return serviceId;
    }

    public UUID professionalId() {
        return professionalId;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public Money price() {
        return new Money(priceCents);
    }

    public int bufferMinutes() {
        return bufferMinutes;
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
        return outro instanceof ServiceOffering oferta && Objects.equals(id, oferta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "ServiceOffering[id=%s, tenantId=%s, serviceId=%s, professionalId=%s]"
                .formatted(id, tenantId, serviceId, professionalId);
    }
}
