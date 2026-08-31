package com.agendaia.organization.application.port.in;

/**
 * Dados do painel do estabelecimento autenticado.
 *
 * <p>Não recebe parâmetro nenhum, de propósito: o tenant vem do
 * {@code TenantContext}, que por sua vez veio da sessão. Aceitar um id aqui
 * abriria a porta para alguém pedir o painel de outro estabelecimento
 * (ADR 0004).
 */
public interface ViewDashboardUseCase {

    DashboardView current();
}
