package com.agendaia.organization.domain;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * O estabelecimento — e o tenant do sistema.
 *
 * <p>Aqui a entidade JPA <strong>é</strong> o modelo, sem classe espelho nem
 * mapper: {@code organization} é subdomínio de suporte (ADR 0002). O regime
 * completo, com domínio puro separado da persistência, é exclusivo de
 * {@code scheduling}.
 *
 * <p>Sem setter público. Criação por {@link #register}, alteração por método de
 * negócio. Um {@code setActive(boolean)} devolveria ao chamador a
 * responsabilidade de manter a invariante.
 */
@Entity
@Table(name = "business")
public class Business {

    /** Fuso assumido quando não perguntado no cadastro. Ver Assumptions da spec funcional. */
    public static final String FUSO_PADRAO = "America/Sao_Paulo";

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60, unique = true)
    private String slug;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected Business() {
        // JPA
    }

    private Business(UUID id, String name, String slug, String timezone, Instant agora) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.timezone = timezone;
        this.active = true;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /**
     * Cria um estabelecimento com o fuso padrão.
     *
     * <p>A identidade é gerada aqui, no domínio — não no {@code INSERT}. É o que
     * permite ao agregado saber quem é antes de qualquer repositório vê-lo
     * (ADR 0009).
     */
    public static Business register(String name, String slug) {
        return register(name, slug, FUSO_PADRAO, Instant.now());
    }

    static Business register(String name, String slug, String timezone, Instant agora) {
        var nomeLimpo = name == null ? "" : name.strip();
        if (nomeLimpo.length() < 2 || nomeLimpo.length() > 120) {
            throw new IllegalArgumentException(
                    "nome do estabelecimento deve ter entre 2 e 120 caracteres");
        }
        if (!SlugGenerator.hasValidFormat(slug)) {
            throw new IllegalArgumentException("slug fora do formato aceito: " + slug);
        }
        // Falha cedo se o fuso não existir: guardar um fuso inválido só apareceria
        // meses depois, na primeira feature que calcular disponibilidade.
        ZoneId.of(timezone);

        return new Business(UuidV7.generate(), nomeLimpo, slug, timezone, agora);
    }

    /**
     * Retira o estabelecimento de circulação sem apagá-lo (ADR 0011): os
     * agendamentos que o referenciam continuam íntegros.
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    /** O id do estabelecimento é o tenant de todas as demais tabelas. */
    public TenantId tenantId() {
        return TenantId.of(id);
    }

    public String name() {
        return name;
    }

    public String slug() {
        return slug;
    }

    public String timezone() {
        return timezone;
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
     * Igualdade por identidade. É seguro aqui porque o id é atribuído na
     * construção e nunca é nulo — ao contrário do padrão em que o banco gera a
     * chave, onde o {@code hashCode} mudaria entre antes e depois do flush.
     */
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Business business && Objects.equals(id, business.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Sem coleção lazy nem dado pessoal: este texto pode acabar em log. */
    @Override
    public String toString() {
        return "Business[id=%s, slug=%s]".formatted(id, slug);
    }
}
