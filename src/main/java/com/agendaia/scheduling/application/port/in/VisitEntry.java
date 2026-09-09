package com.agendaia.scheduling.application.port.in;

import com.agendaia.scheduling.domain.PaymentStatus;
import com.agendaia.shared.Money;
import java.time.Instant;

/** Uma visita (atendimento COMPLETED) no histórico do cliente (gestao-de-clientes, US-2). */
public record VisitEntry(Instant startsAt, String serviceName, Money price, PaymentStatus paymentStatus) {}
