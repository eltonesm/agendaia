package com.agendaia.organization.application.port.in;

import java.util.List;

/**
 * Lista o horário de funcionamento do estabelecimento da sessão.
 *
 * <p>Sem parâmetro, de propósito (DD-1).
 */
public interface ListBusinessOperatingHoursUseCase {

    List<BusinessOperatingHoursView> list();
}
