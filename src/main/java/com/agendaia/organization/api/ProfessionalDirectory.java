package com.agendaia.organization.api;

import java.util.List;

/**
 * Único ponto de entrada de outros contextos em {@code organization}.
 *
 * <p>Sem argumento: tenant lido de {@code TenantContext.require()} por
 * dentro da implementação — mesma extensão do DD-1 de
 * {@code cadastro-profissional}, agora atravessando a fronteira entre
 * contextos (DD-1 de {@code cadastro-servico-oferta}).
 */
public interface ProfessionalDirectory {

    /** Profissionais ativos do tenant da sessão. */
    List<ProfessionalRef> listActive();
}
