package com.agendaia.catalog.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório da oferta.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    /** Usado na listagem. */
    List<ServiceOffering> findByTenantIdAndActiveTrueOrderByCreatedAtAsc(UUID tenantId);

    /** BR-7: uma oferta por (tenant, service, professional). */
    boolean existsByTenantIdAndServiceIdAndProfessionalId(UUID tenantId, UUID serviceId, UUID professionalId);
}
