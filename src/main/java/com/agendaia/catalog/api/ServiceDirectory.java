package com.agendaia.catalog.api;

import java.util.List;

/**
 * Segundo contrato de {@code catalog} — lista de serviços para a página
 * pública (pagina-publica-agendamento, TODO-006, DD-1 da spec técnica).
 *
 * <p>Tenant sempre {@code TenantContext.require()} interno, mesma convenção
 * de {@link ServiceOfferingDirectory}.
 */
public interface ServiceDirectory {

    /** Serviços ativos do tenant, para o cliente escolher o que agendar. */
    List<PublicServiceRef> listActive();
}
