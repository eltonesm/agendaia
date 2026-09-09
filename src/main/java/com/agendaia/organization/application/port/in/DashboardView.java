package com.agendaia.organization.application.port.in;

/**
 * O que o painel exibe.
 *
 * <p>Projeção, não entidade: a tela precisa de três strings, e carregar o
 * agregado inteiro para exibi-las pagaria o custo de tudo que não vai ser usado
 * (ver Performance no PATTERNS.md).
 *
 * <p>Os 4 KPIs do painel (sistema-de-design-admin, TODO-110) **não** entram
 * aqui — ver {@code scheduling.adapter.in.web.DashboardKpiAdvice}. Uma
 * primeira tentativa (DD-6 da spec técnica) pôs
 * {@code scheduling.api.DailyScheduleDirectory} como dependência direta
 * deste record/handler; o `ModuleStructureTest` (Spring Modulith) pegou o
 * ciclo: {@code scheduling} já depende de {@code organization.api}
 * (`ProfessionalDirectory` etc.), então {@code organization} depender de
 * {@code scheduling.api} de volta fecha `organization ⇄ scheduling`. A
 * correção usa o mesmo mecanismo de {@code
 * billing.adapter.in.web.BillingBannerAdvice}: um {@code @ControllerAdvice}
 * que mora em {@code scheduling}, sem {@code organization} precisar saber
 * que {@code scheduling} existe.
 *
 * @param publicUrl endereço completo, pronto para o dono copiar e compartilhar
 */
public record DashboardView(String businessName, String slug, String publicUrl) {}
