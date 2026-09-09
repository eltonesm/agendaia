package com.agendaia.scheduling.api;

import java.time.Instant;

/**
 * Próximo cliente a ser atendido hoje, para o painel do estabelecimento
 * (sistema-de-design-admin, TODO-110).
 */
public record NextClientRef(String customerName, Instant startsAt) {}
