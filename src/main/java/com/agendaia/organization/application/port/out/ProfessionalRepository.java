package com.agendaia.organization.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório do profissional.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {

    /** Usado na listagem. Ordem alfabética: lista que o dono escaneia visualmente. */
    List<Professional> findByTenantIdAndActiveTrueOrderByNameAsc(UUID tenantId);
}
