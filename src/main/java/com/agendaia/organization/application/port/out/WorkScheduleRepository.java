package com.agendaia.organization.application.port.out;

import com.agendaia.organization.domain.WorkSchedule;
import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de saída da jornada.
 *
 * <p>Toda consulta desta feature é por tenant — não existe, e não deveria
 * existir, um {@code findAll()} sem filtro.
 */
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {

    /** Usado na listagem, agrupada por profissional e dia no template. */
    List<WorkSchedule> findByTenantIdAndActiveTrueOrderByProfessionalIdAscDayOfWeekAscStartsAtAsc(UUID tenantId);

    /** Faixas do mesmo profissional no mesmo dia — base da checagem de sobreposição (BR-3, DD-2). */
    List<WorkSchedule> findByTenantIdAndProfessionalIdAndDayOfWeekAndActiveTrue(
            UUID tenantId, UUID professionalId, DayOfWeek dayOfWeek);
}
