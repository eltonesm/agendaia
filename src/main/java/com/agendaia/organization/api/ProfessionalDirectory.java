package com.agendaia.organization.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    /**
     * Um profissional por id, sempre revalidado contra o tenant da sessão —
     * vazio se pertencer a outro tenant ou não existir (DD-8 de
     * confirmacao-e-cancelamento, TODO-007).
     */
    Optional<ProfessionalRef> find(UUID id);
}
