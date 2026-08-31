package com.agendaia.organization.application.port.in;

import java.util.UUID;

/**
 * O que sobrevive do cadastro para o adapter web mostrar.
 *
 * <p>Projeção, não entidade — mesmo padrão de {@code RegisteredBusiness}.
 */
public record RegisteredProfessional(UUID id, String name) {}
