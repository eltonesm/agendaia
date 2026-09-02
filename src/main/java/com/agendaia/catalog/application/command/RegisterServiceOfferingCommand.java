package com.agendaia.catalog.application.command;

import com.agendaia.shared.Money;
import java.util.UUID;

/**
 * Dados do cadastro de oferta.
 *
 * <p>Nenhum campo de tenant — quem determina o estabelecimento é a sessão, no
 * {@code TenantContext}, nunca o que chega no formulário (DD-1).
 */
public record RegisterServiceOfferingCommand(
        UUID serviceId, UUID professionalId, int durationMinutes, Money price, int bufferMinutes) {}
