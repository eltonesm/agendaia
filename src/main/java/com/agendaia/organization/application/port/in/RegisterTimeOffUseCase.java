package com.agendaia.organization.application.port.in;

import com.agendaia.organization.application.command.RegisterTimeOffCommand;

/**
 * Registra um bloqueio do estabelecimento da sessão.
 *
 * <p>Valida o profissional contra o tenant (BR-8) só quando informado —
 * {@code professionalId} nulo vale para o estabelecimento inteiro (DD-3) e
 * não precisa de validação nenhuma.
 */
public interface RegisterTimeOffUseCase {

    RegisteredTimeOff register(RegisterTimeOffCommand command);
}
