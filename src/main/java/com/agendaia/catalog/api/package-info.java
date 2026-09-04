/**
 * Contrato público de {@code catalog} — o único pacote que outros contextos
 * podem importar (ADR 0010).
 *
 * <p>Primeiro contrato de {@code catalog}: {@link com.agendaia.catalog.api.ServiceOfferingDirectory#find(java.util.UUID)},
 * usado por {@code scheduling} para resolver profissional, duração e
 * intervalo a partir do id de uma oferta (consultar-horarios-disponiveis,
 * TODO-005).
 */
@NamedInterface("api")
package com.agendaia.catalog.api;

import org.springframework.modulith.NamedInterface;
