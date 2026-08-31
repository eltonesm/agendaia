package com.agendaia.organization.application.port.in;

import com.agendaia.organization.application.command.RegisterProfessionalCommand;

/**
 * Cadastra um profissional do estabelecimento da sessão.
 *
 * <p>O comando não carrega tenant (DD-1): quem determina o estabelecimento é
 * o {@code TenantContext}, lido por dentro da implementação — nunca um
 * argumento que um chamador futuro pudesse errar.
 */
public interface RegisterProfessionalUseCase {

    RegisteredProfessional register(RegisterProfessionalCommand command);
}
