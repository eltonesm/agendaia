package com.agendaia.catalog.application.port.in;

import java.util.UUID;

/**
 * O que sobrevive do cadastro para o adapter web mostrar.
 *
 * <p>Projeção, não entidade — mesmo padrão de {@code RegisteredProfessional}.
 */
public record RegisteredService(UUID id, String name) {}
