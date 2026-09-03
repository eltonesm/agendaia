package com.agendaia.organization.application.port.out;

import com.agendaia.organization.domain.Professional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de saída do profissional.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {

    /** Usado na listagem. Ordem alfabética: lista que o dono escaneia visualmente. */
    List<Professional> findByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);

    /** Usado para confirmar que um profissional pertence ao tenant da sessão (BR-8, TASK-004). */
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
