package com.agendaia.catalog.application.command;

/**
 * Dados do cadastro de serviço.
 *
 * <p>Nenhum campo de tenant — quem determina o estabelecimento é a sessão, no
 * {@code TenantContext}, nunca o que chega no formulário (DD-1).
 */
public record RegisterServiceCommand(String name, String description) {}
