package com.agendaia.organization.domain;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Quem autentica no painel do estabelecimento.
 *
 * <p>A tabela se chama {@code app_user} porque {@code user} é palavra reservada
 * no PostgreSQL (DD-6): entre aspas funciona, mas {@code SELECT * FROM user}
 * devolve o usuário do banco sem erro nenhum — armadilha permanente para quem
 * depura em produção.
 *
 * <p>Não existe usuário sem estabelecimento: os dois nascem na mesma transação
 * (ADR 0003).
 */
@Entity
@Table(name = "app_user")
public class User {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 254, unique = true)
    private String email;

    @Column(nullable = false, length = 120)
    private String name;

    /** Hash BCrypt. O texto claro nunca chega aqui. */
    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected User() {
        // JPA
    }

    private User(
            UUID id,
            UUID tenantId,
            String email,
            String name,
            String passwordHash,
            UserRole role,
            Instant agora) {
        this.id = id;
        this.tenantId = tenantId;
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /** Cria o dono do estabelecimento — o único papel que existe no MVP. */
    public static User owner(TenantId tenantId, String email, String name, String passwordHash) {
        return owner(tenantId, email, name, passwordHash, Instant.now());
    }

    static User owner(
            TenantId tenantId, String email, String name, String passwordHash, Instant agora) {
        if (tenantId == null) {
            throw new IllegalArgumentException("usuário não existe sem estabelecimento");
        }
        var emailNormalizado = normalize(email);
        if (emailNormalizado.isEmpty() || emailNormalizado.length() > 254) {
            throw new IllegalArgumentException("e-mail inválido");
        }
        var nomeLimpo = name == null ? "" : name.strip();
        if (nomeLimpo.isEmpty() || nomeLimpo.length() > 120) {
            throw new IllegalArgumentException("nome do usuário deve ter entre 1 e 120 caracteres");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("senha já deve chegar aqui como hash");
        }

        return new User(
                UuidV7.generate(),
                tenantId.value(),
                emailNormalizado,
                nomeLimpo,
                passwordHash,
                UserRole.OWNER,
                agora);
    }

    /**
     * Normaliza para minúsculas e remove espaços das pontas.
     *
     * <p>Sem isto, {@code Joao@Exemplo.com } e {@code joao@exemplo.com} viram
     * duas contas — e a restrição de unicidade do banco não impediria.
     */
    private static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    /** Retira de circulação sem apagar (ADR 0011). */
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

    public String email() {
        return email;
    }

    public String name() {
        return name;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public UserRole role() {
        return role;
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
        return outro instanceof User user && Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Nunca inclui e-mail nem hash: e-mail é dado pessoal e isto pode ir para log. */
    @Override
    public String toString() {
        return "User[id=%s, tenantId=%s]".formatted(id, tenantId);
    }
}
