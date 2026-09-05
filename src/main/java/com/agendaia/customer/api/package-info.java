/**
 * Contrato público de {@code customer} — o único pacote que outros
 * contextos podem importar (ADR 0010).
 *
 * <p>{@link com.agendaia.customer.api.CustomerDirectory#findOrCreate}: get-
 * or-create pelo telefone dentro do tenant, chamado de dentro da mesma
 * transação que grava o {@code Appointment} (pagina-publica-agendamento,
 * TODO-006, DD-5 da spec técnica).
 */
@NamedInterface("api")
package com.agendaia.customer.api;

import org.springframework.modulith.NamedInterface;
