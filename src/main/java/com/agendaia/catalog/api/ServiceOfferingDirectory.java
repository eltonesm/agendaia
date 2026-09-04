package com.agendaia.catalog.api;

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
}
