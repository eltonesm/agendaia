package com.agendaia.organization.application.port.in;

import java.util.List;

/**
 * Lista os profissionais do estabelecimento da sessão.
 *
 * <p>Sem parâmetro, de propósito (DD-1) — mesmo raciocínio de
 * {@code ViewDashboardUseCase}: aceitar um id de tenant aqui abriria a porta
 * para alguém pedir a lista de outro estabelecimento.
 */
public interface ListProfessionalsUseCase {

    List<ProfessionalView> list();
}
