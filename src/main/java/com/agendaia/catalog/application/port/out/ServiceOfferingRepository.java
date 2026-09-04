package com.agendaia.catalog.application.port.out;

import com.agendaia.catalog.domain.ServiceOffering;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de saída da oferta.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    /** Usado na listagem. */
    List<ServiceOffering> findByTenantIdAndActiveTrueOrderByCreatedAtAsc(UUID tenantId);

    /** BR-7: uma oferta por (tenant, service, professional). */
    boolean existsByTenantIdAndServiceIdAndProfessionalId(UUID tenantId, UUID serviceId, UUID professionalId);

    /** Usado por {@code ServiceOfferingDirectory.find} (consultar-horarios-disponiveis, BR-7). */
    Optional<ServiceOffering> findByTenantIdAndIdAndActiveTrue(UUID tenantId, UUID id);
}
