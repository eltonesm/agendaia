package com.agendaia.customer.application.port.out;

import com.agendaia.customer.domain.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de saída de {@link Customer}.
 *
 * <p>Toda consulta é por tenant — telefone só é chave natural {@code dentro
 * do} tenant (BR-3 da spec funcional de pagina-publica-agendamento).
 */
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /** Get-or-create pelo par (tenant, telefone) — chave natural do agregado. */
    Optional<Customer> findByTenantIdAndPhone(UUID tenantId, String phone);
}
