package com.agendaia.catalog.application.port.in;

import java.util.List;

/**
 * Lista os serviços do estabelecimento da sessão.
 *
 * <p>Sem parâmetro, de propósito (DD-1): aceitar um id de tenant aqui abriria
 * a porta para alguém pedir a lista de outro estabelecimento.
 */
public interface ListServicesUseCase {

    List<ServiceView> list();
}
