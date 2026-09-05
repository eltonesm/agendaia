package com.agendaia.catalog.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Único ponto de entrada de outros contextos em {@code catalog}.
 *
 * <p>Sem tenant no argumento: lido de {@code TenantContext.require()} por
 * dentro da implementação — mesma convenção de
 * {@code organization.api.ProfessionalDirectory} (DD-1 de
 * cadastro-servico-oferta), agora aplicada ao primeiro contrato de
 * {@code catalog}.
 */
public interface ServiceOfferingDirectory {

    /** Vazio se o id não existe, ou existe em outro tenant (BR-7). */
    Optional<ServiceOfferingRef> find(UUID serviceOfferingId);

    /**
     * Ofertas ativas de um serviço, para a página pública (pagina-publica-
     * agendamento, TODO-006, DD-1 da spec técnica) — o cliente escolhe o
     * profissional a partir desta lista.
     */
    List<PublicOfferingRef> listActiveByService(UUID serviceId);
}
