package com.agendaia.catalog.domain;

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
 * O conceito vendável ("Corte de Cabelo") — primeiro agregado de
 * {@code catalog}. Sem preço nem duração: isso é de {@link ServiceOffering}.
 *
 * <p>Mesmo regime CRUD de {@code organization} (ADR 0002): a entidade JPA é
 * o modelo, sem classe espelho nem mapper. Sem setter público; criação por
 * {@link #register}.
 */
@Entity
@Table(name = "service")
public class Service {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected Service() {
        // JPA
    }

    private Service(UUID id, UUID tenantId, String name, String description, Instant agora) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.active = true;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /**
     * Cadastra um serviço do estabelecimento da sessão. {@code description}
     * é opcional (BR-1 exige só o nome, único por tenant — a unicidade é
     * garantida pelo banco, não aqui).
     */
    public static Service register(TenantId tenantId, String name, String description) {
        return register(tenantId, name, description, Instant.now());
    }

    static Service register(TenantId tenantId, String name, String description, Instant agora) {
        if (tenantId == null) {
            throw new IllegalArgumentException("serviço não existe sem estabelecimento");
        }
        var nomeLimpo = name == null ? "" : name.strip();
        if (nomeLimpo.length() < 2 || nomeLimpo.length() > 120) {
            throw new IllegalArgumentException("nome do serviço deve ter entre 2 e 120 caracteres");
        }
        var descricaoLimpa = description == null ? null : description.strip();
        if (descricaoLimpa != null && descricaoLimpa.length() > 500) {
            throw new IllegalArgumentException("descrição do serviço deve ter no máximo 500 caracteres");
        }
        if (descricaoLimpa != null && descricaoLimpa.isEmpty()) {
            descricaoLimpa = null;
        }
        return new Service(UuidV7.generate(), tenantId.value(), nomeLimpo, descricaoLimpa, agora);
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

    public String description() {
        return description;
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
        return outro instanceof Service service && Objects.equals(id, service.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Nome não é dado sensível — pode ir para log sem problema. */
    @Override
    public String toString() {
        return "Service[id=%s, tenantId=%s, name=%s]".formatted(id, tenantId, name);
    }
}
