package com.agendaia.organization.application.port.in;

import java.util.List;

/**
 * Lista as faixas de jornada do estabelecimento da sessão, de todos os
 * profissionais.
 *
 * <p>Sem parâmetro, de propósito (DD-1).
 */
public interface ListWorkSchedulesUseCase {

    List<WorkScheduleView> list();
}
