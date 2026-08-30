package com.agendaia.shared;

import java.util.UUID;

/**
 * Identificador do tenant — no AgendaIA, o estabelecimento.
 *
 * <p>Existe como tipo, e não como {@link UUID} cru, por uma razão prática: com
 * {@code UUID} em toda assinatura, nada impede passar um {@code professionalId}
 * onde se espera um {@code tenantId}. Compila, roda, e vaza dado entre
 * estabelecimentos. Com tipo próprio, o compilador recusa.
 *
 * <p>É separado de {@code BusinessId} de propósito, embora hoje os dois valores
 * coincidam (ADR 0003): o isolamento não deve depender de essa coincidência
 * continuar valendo.
 */
public record TenantId(UUID value) {

    public TenantId {
        if (value == null) {
            throw new IllegalArgumentException("tenantId não pode ser nulo");
        }
    }

    public static TenantId of(UUID value) {
        return new TenantId(value);
    }

    /** Útil para converter o que vem da sessão ou do banco. */
    public static TenantId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("tenantId não pode ser vazio");
        }
        return new TenantId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
