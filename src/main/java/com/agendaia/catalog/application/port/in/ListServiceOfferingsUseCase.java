package com.agendaia.catalog.application.port.in;

import java.util.List;

/**
 * Lista as ofertas do estabelecimento da sessão.
 *
 * <p>Sem parâmetro, de propósito (DD-1).
 */
public interface ListServiceOfferingsUseCase {

    List<ServiceOfferingView> list();
}
