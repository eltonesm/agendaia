package com.agendaia.billing.adapter.in.web.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Marcar como pago e estender prazo são a mesma ação (BR-3, DD-7 da spec
 * técnica) — um campo só, a nova data de validade de acesso.
 */
public record ExtendAccessRequest(@NotNull @Future LocalDate accessValidUntil) {}
