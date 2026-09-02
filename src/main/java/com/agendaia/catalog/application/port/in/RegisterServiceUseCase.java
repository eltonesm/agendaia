package com.agendaia.catalog.application.port.in;

import com.agendaia.catalog.application.command.RegisterServiceCommand;

/**
 * Cadastra um serviço do estabelecimento da sessão.
 *
 * <p>O comando não carrega tenant (DD-1): quem determina o estabelecimento é
 * o {@code TenantContext}, lido por dentro da implementação.
 */
public interface RegisterServiceUseCase {

    RegisteredService register(RegisterServiceCommand command);
}
