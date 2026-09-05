package com.agendaia.organization.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Segundo ponto de entrada de outros contextos em {@code organization},
 * e o único **sem** tenant implícito.
 *
 * <p>{@link ProfessionalDirectory} e {@code AvailabilityDirectory} sempre
 * leem {@code TenantContext.require()} internamente — fazem sentido só
 * dentro de uma sessão de dono. Os dois métodos aqui são o oposto: recebem
 * o tenant como argumento, de propósito (DD-5 da spec técnica de
 * back-office-operador):
 *
 * <ul>
 *   <li>{@link #listAll()} — existe para quem **não tem** tenant nenhum, o
 *       operador da plataforma. Só é chamado por código atrás do login
 *       isolado do operador.</li>
 *   <li>{@link #find(UUID)} — existe para {@code billing} resolver a data
 *       de cadastro de **um** estabelecimento (criação da conta de
 *       cobrança sob demanda), sempre com o {@code tenantId} já resolvido
 *       pela própria sessão do dono via {@code TenantContext} — nunca um
 *       id escolhido livremente pelo chamador.</li>
 * </ul>
 */
public interface BusinessDirectory {

    /** Todos os estabelecimentos cadastrados, sem filtro nenhum. */
    List<BusinessRef> listAll();

    /** Vazio se o id não corresponde a nenhum estabelecimento. */
    Optional<BusinessRef> find(UUID tenantId);
}
