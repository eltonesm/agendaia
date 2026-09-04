package com.agendaia.billing.application.port.out;

import com.agendaia.billing.domain.BillingAccount;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de saída da conta de cobrança.
 *
 * <p>Uma conta por tenant — {@link #findByTenantId} é a consulta central
 * (get-or-create, DD-2 da spec técnica). {@link #findByTenantIdIn} existe
 * para o painel do operador buscar todas de uma vez, sem uma consulta por
 * estabelecimento.
 */
public interface BillingAccountRepository extends JpaRepository<BillingAccount, UUID> {

    Optional<BillingAccount> findByTenantId(UUID tenantId);

    List<BillingAccount> findByTenantIdIn(Collection<UUID> tenantIds);
}
