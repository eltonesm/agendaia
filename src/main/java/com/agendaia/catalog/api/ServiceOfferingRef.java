package com.agendaia.catalog.api;

import java.util.UUID;

/**
 * Projeção de {@code ServiceOffering} exportada para outros contextos.
 *
 * <p>Igual em forma a {@code ServiceOfferingView} (uso interno de
 * {@code catalog}), mas um tipo diferente — mesmo raciocínio de
 * {@code organization.api.ProfessionalRef}: um pertence à {@code api}, o
 * outro a {@code application.port.in}, e podem divergir livremente no
 * futuro.
 */
public record ServiceOfferingRef(UUID id, UUID professionalId, int durationMinutes, int bufferMinutes) {}
