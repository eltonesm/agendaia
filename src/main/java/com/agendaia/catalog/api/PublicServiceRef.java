package com.agendaia.catalog.api;

import java.util.UUID;

/**
 * Projeção de {@code Service} para a página pública (pagina-publica-
 * agendamento, TODO-006). Igual em forma a {@code ServiceView} (uso interno
 * do admin), mas um tipo diferente — podem divergir livremente no futuro
 * (mesmo raciocínio de {@code ProfessionalRef}/{@code ProfessionalView}).
 */
public record PublicServiceRef(UUID id, String name) {}
