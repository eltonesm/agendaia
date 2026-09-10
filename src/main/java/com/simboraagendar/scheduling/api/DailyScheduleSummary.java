package com.simboraagendar.scheduling.api;

import com.simboraagendar.shared.Money;

/**
 * Resumo do dia para o painel do estabelecimento (sistema-de-design-admin,
 * TODO-110). {@code nextClient} é {@code null} quando não há mais nenhum
 * agendamento ativo hoje.
 *
 * @param totalToday agendamentos de hoje em SCHEDULED, CONFIRMED ou COMPLETED
 * @param completedToday agendamentos de hoje em COMPLETED
 * @param estimatedRevenue soma do preço dos agendamentos de hoje em SCHEDULED, CONFIRMED ou COMPLETED
 */
public record DailyScheduleSummary(
        long totalToday, long completedToday, Money estimatedRevenue, NextClientRef nextClient) {}
