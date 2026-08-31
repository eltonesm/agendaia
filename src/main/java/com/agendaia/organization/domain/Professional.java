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
 * Quem atende no estabelecimento — o segundo agregado de {@code organization}.
 *
 * <p>Mesmo regime CRUD de {@link Business} e {@link User} (ADR 0002): a
 * entidade JPA é o modelo, sem classe espelho nem mapper. Sem setter público;
 * criação por {@link #register}, alteração por método de negócio.
 *
 * <p><strong>Pode ou não ter um {@link User} associado.</strong> Nesta feature
 * não tem nenhum — login de profissional é fora de escopo (spec funcional).
 * Um profissional é, por enquanto, só um registro que o dono gerencia.
 */
@Entity
@Table(name = "professional")
public class Professional {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected Professional() {
        // JPA
    }

    private Professional(UUID id, UUID tenantId, String name, Instant agora) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.active = true;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /**
     * Cadastra um profissional do estabelecimento da sessão.
     *
     * <p>Recebe {@link TenantId}, nunca {@link UUID} cru — o tipo próprio
     * existe exatamente para que o compilador recuse passar, por engano, o id
     * de outra coisa onde se espera o tenant (ver javadoc de
     * {@code TenantId}).
     */
    public static Professional register(TenantId tenantId, String name) {
        return register(tenantId, name, Instant.now());
    }

    static Professional register(TenantId tenantId, String name, Instant agora) {
        if (tenantId == null) {
            throw new IllegalArgumentException("profissional não existe sem estabelecimento");
        }
        var nomeLimpo = name == null ? "" : name.strip();
        if (nomeLimpo.length() < 2 || nomeLimpo.length() > 120) {
            throw new IllegalArgumentException(
                    "nome do profissional deve ter entre 2 e 120 caracteres");
        }
        return new Professional(UuidV7.generate(), tenantId.value(), nomeLimpo, agora);
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

    public String name() {
        return name;
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
        return outro instanceof Professional professional && Objects.equals(id, professional.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Nome não é dado sensível — pode ir para log sem problema. */
    @Override
    public String toString() {
        return "Professional[id=%s, tenantId=%s, name=%s]".formatted(id, tenantId, name);
    }
}
