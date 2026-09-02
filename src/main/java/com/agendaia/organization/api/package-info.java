/**
 * Contrato público de {@code organization} — o único pacote que outros
 * contextos podem importar (ADR 0010).
 *
 * <p>Hoje, uma operação só: {@link com.agendaia.organization.api.ProfessionalDirectory#listActive()}.
 * Grossa, em lote, sem N+1 — mesmo profissional serve para popular um
 * dropdown e para validar um id recebido (DD-1 da spec técnica de
 * cadastro-servico-oferta).
 */
@NamedInterface("api")
package com.agendaia.organization.api;

import org.springframework.modulith.NamedInterface;
