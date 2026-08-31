package com.agendaia.organization.application.command;

/**
 * Dados do cadastro de profissional.
 *
 * <p>Só o nome. Nenhum campo de tenant — quem determina o estabelecimento é a
 * sessão, no {@code TenantContext}, nunca o que chega no formulário (DD-1).
 */
public record RegisterProfessionalCommand(String name) {}
