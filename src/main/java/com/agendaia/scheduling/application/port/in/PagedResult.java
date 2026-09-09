package com.agendaia.scheduling.application.port.in;

import java.util.List;

/**
 * Página de resultado, sem depender de {@code org.springframework.data.domain.Page}
 * (gestao-de-clientes, DD-4) — um {@code port.in} não conhece framework, mesmo
 * espírito de {@code domain} nunca conhecer JPA.
 */
public record PagedResult<T>(List<T> items, int page, int size, long totalElements) {}
