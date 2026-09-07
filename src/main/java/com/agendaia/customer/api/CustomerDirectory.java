package com.agendaia.customer.api;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Único ponto de entrada de outros contextos em {@code customer}.
 *
 * <p>Tenant sempre {@code TenantContext.require()} interno — quem chama já
 * está dentro de uma requisição com tenant resolvido (pela sessão do dono
 * ou, na página pública, pelo slug).
 */
public interface CustomerDirectory {

    /**
     * Cria ou reaproveita o cliente pelo telefone dentro do tenant (BR-3 da
     * spec funcional de pagina-publica-agendamento). O nome mais recente
     * informado atualiza o cadastro — o telefone é a chave, não o nome.
     */
    UUID findOrCreate(String name, String phone);

    /**
     * Um cliente por id, sempre revalidado contra o tenant da sessão —
     * vazio se pertencer a outro tenant ou não existir (DD-8 de
     * confirmacao-e-cancelamento, TODO-007).
     */
    Optional<CustomerRef> find(UUID id);

    /**
     * Clientes de um lote de ids, sempre revalidados contra o tenant da
     * sessão — uma única consulta, nunca uma por agendamento (agenda-
     * profissional, TODO-008, DD-4). Ids que não existem, ou existem em
     * outro tenant, simplesmente não aparecem no resultado.
     */
    List<CustomerRef> findByIds(Collection<UUID> ids);
}
