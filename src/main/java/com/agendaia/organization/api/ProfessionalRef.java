package com.agendaia.organization.api;

import java.util.UUID;

/**
 * Projeção de {@code Professional} exportada para outros contextos.
 *
 * <p>Igual em forma a {@code ProfessionalView} (uso interno de
 * {@code organization}), mas um tipo diferente — um pertence à {@code api},
 * o outro a {@code application.port.in}, e podem divergir livremente no
 * futuro. Tipo de domínio interno nunca atravessa a fronteira
 * ({@code PATTERNS.md}).
 */
public record ProfessionalRef(UUID id, String name) {}
