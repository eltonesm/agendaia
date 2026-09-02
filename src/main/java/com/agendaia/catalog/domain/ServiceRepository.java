package com.agendaia.catalog.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório do serviço.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface ServiceRepository extends JpaRepository<Service, UUID> {

    /** Usado na listagem. Ordem alfabética: lista que o dono escaneia visualmente. */
    List<Service> findByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);
}
