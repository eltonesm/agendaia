/**
 * Contrato público de {@code organization} — o único pacote que outros
 * contextos podem importar (ADR 0010).
 *
 * <p>{@link com.agendaia.organization.api.ProfessionalDirectory#listActive()}:
 * grossa, em lote, sem N+1 — mesmo profissional serve para popular um
 * dropdown e para validar um id recebido (DD-1 da spec técnica de
 * cadastro-servico-oferta).
 *
 * <p>{@link com.agendaia.organization.api.AvailabilityDirectory}: dado
 * declarado de disponibilidade (horário de funcionamento, jornada,
 * bloqueio), sempre convertido para {@code shared.TimeRange} antes de
 * atravessar a fronteira (consultar-horarios-disponiveis, TODO-005, DD-4).
 */
@NamedInterface("api")
package com.agendaia.organization.api;

import org.springframework.modulith.NamedInterface;
