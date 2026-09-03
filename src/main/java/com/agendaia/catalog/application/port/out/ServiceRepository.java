package com.agendaia.catalog.application.port.out;

import com.agendaia.catalog.domain.Service;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de saída do serviço.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    /** Usado na listagem. Ordem alfabética: lista que o dono escaneia visualmente. */
    List<Service> findByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);

    /** Verificação antecipada de nome duplicado (BR-1), antes do INSERT. */
    boolean existsByTenantIdAndName(UUID tenantId, String name);

    /** Usado para confirmar que um serviço pertence ao tenant da sessão (BR-8-like, TASK-007). */
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
