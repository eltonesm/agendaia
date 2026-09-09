package com.agendaia.scheduling.api;

import java.time.LocalDate;

/**
 * KPIs do dia para o painel do estabelecimento (sistema-de-design-admin,
 * TODO-110). Único ponto de entrada de {@code organization} em
 * {@code scheduling}.
 */
public interface DailyScheduleDirectory {

    /** Tenant vem de {@code TenantContext.require()} — nunca argumento (ADR 0004). */
    DailyScheduleSummary summaryFor(LocalDate date);
}
