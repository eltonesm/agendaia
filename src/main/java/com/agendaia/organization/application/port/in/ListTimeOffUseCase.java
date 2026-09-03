package com.agendaia.organization.application.port.in;

import java.util.List;

/**
 * Lista os bloqueios do estabelecimento da sessão.
 *
 * <p>Sem parâmetro, de propósito (DD-1).
 */
public interface ListTimeOffUseCase {

    List<TimeOffView> list();
}
