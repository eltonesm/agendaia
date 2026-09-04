package com.agendaia.billing.domain;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Uma por estabelecimento — guarda até quando o acesso ao painel
 * administrativo é válido. Nunca dado de cartão, nunca gateway (glossário,
 * "Contexto Billing").
 *
 * <p>Mesmo regime CRUD de {@code Professional}/{@code WorkSchedule} (ADR
 * 0002). Sem setter público além de {@link #extendUntil}; criação por
 * {@link #startTrial}.
 *
 * <p><strong>{@code trialEndsAt} é imutável</strong> (DD-6 da spec
 * técnica) — gravado uma vez, no cadastro, nunca muda depois. É o que
 * permite {@link #statusOn} distinguir {@code TRIAL} de {@code PAID}: se
 * {@code accessValidUntil} já passou de {@code trialEndsAt}, alguém marcou
 * pagamento em algum momento.
 */
@Entity
@Table(name = "billing_account")
public class BillingAccount {

    private static final int TRIAL_DAYS = 30;
    private static final int GRACE_PERIOD_DAYS = 5;

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "trial_ends_at", nullable = false)
    private LocalDate trialEndsAt;

    @Column(name = "access_valid_until", nullable = false)
    private LocalDate accessValidUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected BillingAccount() {
        // JPA
    }

    private BillingAccount(
            UUID id, UUID tenantId, LocalDate trialEndsAt, LocalDate accessValidUntil, Instant agora) {
        this.id = id;
        this.tenantId = tenantId;
        this.trialEndsAt = trialEndsAt;
        this.accessValidUntil = accessValidUntil;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /**
     * Inicia o teste gratuito de 30 dias corridos a partir da data de
     * cadastro do estabelecimento (BR-1). Cobre também o cadastro
     * retroativo de estabelecimentos anteriores a esta feature (BR-8) —
     * quem chama decide qual {@code registeredOn} usar.
     */
    public static BillingAccount startTrial(UUID tenantId, LocalDate registeredOn) {
        return startTrial(tenantId, registeredOn, Instant.now());
    }

    static BillingAccount startTrial(UUID tenantId, LocalDate registeredOn, Instant agora) {
        if (tenantId == null) {
            throw new IllegalArgumentException("conta de cobrança não existe sem estabelecimento");
        }
        if (registeredOn == null) {
            throw new IllegalArgumentException("conta de cobrança precisa da data de cadastro");
        }
        var fimDoTeste = registeredOn.plusDays(TRIAL_DAYS);
        return new BillingAccount(UuidV7.generate(), tenantId, fimDoTeste, fimDoTeste, agora);
    }

    /**
     * Marca pagamento recebido ou estende o prazo — a mesma ação para os
     * dois casos (BR-3, DD-7 da spec técnica). {@code newDate} precisa ser
     * estritamente posterior a hoje.
     */
    public void extendUntil(LocalDate newDate) {
        extendUntil(newDate, LocalDate.now());
    }

    void extendUntil(LocalDate newDate, LocalDate hoje) {
        if (newDate == null || !newDate.isAfter(hoje)) {
            throw new IllegalArgumentException("nova data de validade precisa ser posterior a hoje");
        }
        this.accessValidUntil = newDate;
        this.updatedAt = Instant.now();
    }

    /**
     * Status calculado a partir de {@code accessValidUntil} e da data
     * informada — nunca gravado à parte (BR-2).
     */
    public AccessStatus statusOn(LocalDate today) {
        if (!today.isAfter(accessValidUntil)) {
            return accessValidUntil.isAfter(trialEndsAt) ? AccessStatus.PAID : AccessStatus.TRIAL;
        }
        var fimDaCarencia = accessValidUntil.plusDays(GRACE_PERIOD_DAYS);
        return today.isAfter(fimDaCarencia) ? AccessStatus.BLOCKED : AccessStatus.GRACE_PERIOD;
    }

    public UUID id() {
        return id;
    }

    public TenantId tenantId() {
        return TenantId.of(tenantId);
    }

    public LocalDate trialEndsAt() {
        return trialEndsAt;
    }

    public LocalDate accessValidUntil() {
        return accessValidUntil;
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
        return outro instanceof BillingAccount conta && Objects.equals(id, conta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "BillingAccount[id=%s, tenantId=%s, trialEndsAt=%s, accessValidUntil=%s]"
                .formatted(id, tenantId, trialEndsAt, accessValidUntil);
    }
}
