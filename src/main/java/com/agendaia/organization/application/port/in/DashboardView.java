package com.agendaia.organization.application.port.in;

/**
 * O que o painel exibe.
 *
 * <p>Projeção, não entidade: a tela precisa de três strings, e carregar o
 * agregado inteiro para exibi-las pagaria o custo de tudo que não vai ser usado
 * (ver Performance no PATTERNS.md).
 *
 * @param publicUrl endereço completo, pronto para o dono copiar e compartilhar
 */
public record DashboardView(String businessName, String slug, String publicUrl) {}
