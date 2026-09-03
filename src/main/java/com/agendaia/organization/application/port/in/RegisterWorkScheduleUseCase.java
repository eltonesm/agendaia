package com.agendaia.organization.application.port.in;

import com.agendaia.organization.application.command.RegisterWorkScheduleCommand;

/**
 * Declara uma faixa de jornada de um profissional do estabelecimento da
 * sessão.
 *
 * <p>Valida que o profissional pertence ao tenant da sessão (BR-8) e recusa
 * faixa sobreposta a outra do mesmo profissional no mesmo dia (BR-3).
 */
public interface RegisterWorkScheduleUseCase {

    RegisteredWorkSchedule register(RegisterWorkScheduleCommand command);
}
