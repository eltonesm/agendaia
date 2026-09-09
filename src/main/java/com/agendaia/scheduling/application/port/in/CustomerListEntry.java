package com.agendaia.scheduling.application.port.in;

import java.util.UUID;

/**
 * Uma linha da lista de clientes (gestao-de-clientes, US-1). {@code isNew}
 * já calculado pelo handler, nunca pelo controller/template (PATTERNS.md) —
 * mesmo raciocínio de {@link AgendaEntry}.
 */
public record CustomerListEntry(UUID customerId, String name, String phone, long visitCount, boolean isNew) {}
