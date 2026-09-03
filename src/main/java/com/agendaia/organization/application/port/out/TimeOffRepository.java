package com.agendaia.organization.application.port.out;

import com.agendaia.organization.domain.TimeOff;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de saída do bloqueio.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface TimeOffRepository extends JpaRepository<TimeOff, UUID> {

    /** Usado na listagem, mais recente primeiro. */
    List<TimeOff> findByTenantIdAndActiveTrueOrderByStartsAtDesc(UUID tenantId);
}
