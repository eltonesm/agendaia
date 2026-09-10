package com.simboraagendar.scheduling.application.port.out;

import com.simboraagendar.shared.Money;

/**
 * Atividade agregada de um cliente (gestao-de-clientes) — um por
 * {@code customerId}, resultado de {@link AppointmentRepository#findActivityByCustomerIds}.
 * Considera só agendamentos {@code COMPLETED} (BR-1/BR-7/BR-8/BR-9 da spec
 * funcional).
 */
public record CustomerActivity(long visitCount, Money totalSpent, Money totalOwed) {}
