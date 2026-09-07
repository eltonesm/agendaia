package com.agendaia.customer.api;

import java.util.UUID;

/**
 * Projeção de {@code Customer} exportada para outros contextos.
 *
 * <p>{@code phone} ganho em agenda-profissional (TODO-008, DD-4) — a tela
 * admin exibe o telefone do cliente na lista de agendamentos; a tela pública
 * (confirmacao-e-cancelamento, TODO-007) simplesmente não o usa.
 */
public record CustomerRef(UUID id, String name, String phone) {}
