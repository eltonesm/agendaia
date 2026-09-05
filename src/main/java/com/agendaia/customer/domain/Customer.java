package com.agendaia.customer.domain;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * A pessoa atendida pelo estabelecimento (glossário) — nome e telefone, sem
 * login no MVP.
 *
 * <p>Mesmo regime CRUD de {@code Business}/{@code Service} (ADR 0002,
 * DD-2 da spec técnica de pagina-publica-agendamento): a entidade JPA é o
 * modelo, sem classe espelho nem mapper. Sem setter público além de
 * {@link #renameTo}; criação por {@link #register}.
 *
 * <p>Telefone normalizado em E.164 é a chave natural dentro do tenant
 * (glossário) — a unicidade é garantida pelo banco ({@code UNIQUE(tenant_id,
 * phone)}), não aqui.
 */
@Entity
@Table(name = "customer")
public class Customer {

    /** Aceita dígitos com "+" opcional na frente — validação de formato, não de operadora/DDD real. */
    private static final Pattern TELEFONE_VALIDO = Pattern.compile("^\\+?\\d{8,15}$");

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "anonymized_at")
    private Instant anonymizedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Exigido pelo Hibernate. Ninguém mais deve usar. */
    protected Customer() {
        // JPA
    }

    private Customer(UUID id, UUID tenantId, String name, String phone, Instant agora) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.phone = phone;
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    /** Cadastra um cliente novo do estabelecimento — chamado só pelo get-or-create do formulário público. */
    public static Customer register(TenantId tenantId, String name, String phone) {
        return register(tenantId, name, phone, Instant.now());
    }

    static Customer register(TenantId tenantId, String name, String phone, Instant agora) {
        if (tenantId == null) {
            throw new IllegalArgumentException("cliente não existe sem estabelecimento");
        }
        var nomeLimpo = validarNome(name);
        var telefoneLimpo = normalizePhone(phone);
        return new Customer(UuidV7.generate(), tenantId.value(), nomeLimpo, telefoneLimpo, agora);
    }

    /**
     * Normaliza para o mesmo formato usado na gravação — quem vai buscar
     * pelo telefone (get-or-create) precisa comparar com o mesmo valor que
     * {@link #register} grava, nunca com a string bruta do formulário.
     */
    public static String normalizePhone(String phone) {
        var telefoneLimpo = phone == null ? "" : phone.strip();
        if (!TELEFONE_VALIDO.matcher(telefoneLimpo).matches()) {
            throw new IllegalArgumentException("telefone fora do formato aceito: " + phone);
        }
        return telefoneLimpo;
    }

    /**
     * Telefone é a chave, não o nome (BR-3 da spec funcional de
     * pagina-publica-agendamento) — um novo agendamento com nome diferente
     * atualiza o cadastro em vez de criar um segundo cliente.
     */
    public void renameTo(String name) {
        this.name = validarNome(name);
        this.updatedAt = Instant.now();
    }

    private static String validarNome(String name) {
        var nomeLimpo = name == null ? "" : name.strip();
        if (nomeLimpo.length() < 2 || nomeLimpo.length() > 120) {
            throw new IllegalArgumentException("nome do cliente deve ter entre 2 e 120 caracteres");
        }
        return nomeLimpo;
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

    public String phone() {
        return phone;
    }

    public Instant anonymizedAt() {
        return anonymizedAt;
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
        return outro instanceof Customer customer && Objects.equals(id, customer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Sem telefone/nome no toString — dado pessoal não deve acabar em log (LGPD). */
    @Override
    public String toString() {
        return "Customer[id=%s, tenantId=%s]".formatted(id, tenantId);
    }
}
