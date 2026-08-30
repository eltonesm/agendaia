package com.agendaia.organization.application.port.in;

import com.agendaia.shared.TenantId;
import java.util.UUID;

/**
 * O que o cadastro produziu.
 *
 * <p>Record imutável, com o mínimo que a camada web precisa para autenticar a
 * sessão e montar o painel. Não devolve a entidade: fora da transação ela
 * estaria destacada, e expor o agregado convidaria o controller a navegar por
 * ele.
 */
public record RegisteredBusiness(
        UUID businessId, TenantId tenantId, String businessName, String slug, String ownerEmail) {}
