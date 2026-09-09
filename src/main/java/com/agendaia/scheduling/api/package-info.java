/**
 * Contrato público de {@code scheduling} — o único pacote que outros
 * contextos podem importar (ADR 0010).
 *
 * <p>Primeiro contrato de {@code scheduling}: até a TODO-110
 * (sistema-de-design-admin), este contexto só consumia {@code
 * organization.api}/{@code catalog.api}/{@code customer.api}, nunca expôs
 * nada para fora. {@link com.agendaia.scheduling.api.DailyScheduleDirectory}
 * existe porque o painel do estabelecimento (contexto {@code organization})
 * precisa dos KPIs do dia — dado que só {@code scheduling} possui — sem
 * importar {@code scheduling.application} nem {@code scheduling.domain}.
 */
@NamedInterface("api")
package com.agendaia.scheduling.api;

import org.springframework.modulith.NamedInterface;
