/**
 * Contexto delimitado Billing — prazo de acesso e status de pagamento de
 * cada estabelecimento (TODO-009).
 *
 * <p>Dono de {@code BillingAccount}: uma conta por tenant, guardando até
 * quando o acesso ao painel administrativo é válido. Regime CRUD (ADR
 * 0002) — subdomínio de suporte, sem regra que precise de teste isolado de
 * framework.
 *
 * <p>É a "decisão nova" que o glossário previa antes de liberar
 * {@code Plano}/{@code Assinatura}/{@code Pagamento} — ver
 * {@code docs/domain/glossary.md}, seção "Contexto Billing". Sem gateway
 * de pagamento: Pix é recebido por fora, o operador só registra
 * manualmente (ver ADR e {@code architecture-haiku.md}).
 *
 * <p>Único contexto que também hospeda mecanismos transversais de negócio
 * ({@code AccessGuardFilter}, {@code BillingBannerAdvice}) — eles não
 * moram em {@code platform} porque decidir "este estabelecimento está
 * bloqueado" é regra de negócio, e {@code platform} proíbe isso no seu
 * próprio {@code package-info.java}.
 */
@ApplicationModule(
        displayName = "Billing",
        allowedDependencies = {"organization :: api", "shared", "platform"})
package com.agendaia.billing;

import org.springframework.modulith.ApplicationModule;
