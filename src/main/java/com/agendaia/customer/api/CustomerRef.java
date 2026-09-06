package com.agendaia.customer.api;

import java.util.UUID;

/**
 * Projeção de {@code Customer} exportada para outros contextos.
 *
 * <p>Só o nome — sem telefone, que não é necessário a quem só precisa
 * exibir o nome do cliente (confirmacao-e-cancelamento, DD-8).
 */
public record CustomerRef(UUID id, String name) {}
