package com.agendaia.organization.application.port.out;

import com.agendaia.organization.domain.BusinessOperatingHours;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de saída do horário de funcionamento.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface BusinessOperatingHoursRepository extends JpaRepository<BusinessOperatingHours, UUID> {

    /** Usado na listagem, agrupada por dia da semana no template. */
    List<BusinessOperatingHours> findByTenantIdAndActiveTrueOrderByDayOfWeekAscOpensAtAsc(UUID tenantId);
}
