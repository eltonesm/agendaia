package com.simboraagendar.scheduling.application.port.in;

import com.simboraagendar.shared.Money;
import java.util.List;
import java.util.UUID;

/** Detalhe completo de um cliente (gestao-de-clientes, US-2): totais e histórico de visitas. */
public record CustomerActivityDetail(
        UUID customerId,
        String name,
        String phone,
        long visitCount,
        Money totalSpent,
        Money totalOwed,
        List<VisitEntry> visits) {}
