package com.agendaia.organization.application.port.in;

import com.agendaia.organization.application.command.RegisterBusinessOperatingHoursCommand;

/**
 * Declara uma faixa de horário de funcionamento do estabelecimento da sessão.
 *
 * <p>O comando não carrega tenant (DD-1): quem determina o estabelecimento é
 * o {@code TenantContext}, lido por dentro da implementação.
 */
public interface RegisterBusinessOperatingHoursUseCase {

    RegisteredBusinessOperatingHours register(RegisterBusinessOperatingHoursCommand command);
}
