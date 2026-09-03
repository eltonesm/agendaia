package com.agendaia.organization.application;

import com.agendaia.organization.application.port.in.ListWorkSchedulesUseCase;
import com.agendaia.organization.application.port.in.WorkScheduleView;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.WorkSchedule;
import com.agendaia.platform.tenant.TenantContext;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista as faixas de jornada do estabelecimento da sessão, com o nome do
 * profissional já resolvido.
 *
 * <p>Tenant lido do {@link TenantContext}, nunca de argumento (DD-1).
 * {@code Professional} é do mesmo contexto — resolvido por lote
 * ({@code findAllById}), nunca uma consulta por faixa.
 */
@Service
public class ListWorkSchedulesHandler implements ListWorkSchedulesUseCase {

    private final WorkScheduleRepository workScheduleRepository;
    private final ProfessionalRepository professionalRepository;

    public ListWorkSchedulesHandler(
            WorkScheduleRepository workScheduleRepository, ProfessionalRepository professionalRepository) {
        this.workScheduleRepository = workScheduleRepository;
        this.professionalRepository = professionalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkScheduleView> list() {
        var tenantId = TenantContext.require();

        var faixas = workScheduleRepository.findByTenantIdAndActiveTrueOrderByProfessionalIdAscDayOfWeekAscStartsAtAsc(
                tenantId.value());

        var idsDeProfissional = faixas.stream().map(WorkSchedule::professionalId).distinct().toList();
        var nomesDeProfissional = professionalRepository.findAllById(idsDeProfissional).stream()
                .collect(Collectors.toMap(Professional::id, Professional::name));

        return faixas.stream()
                .map(faixa -> new WorkScheduleView(
                        faixa.id(),
                        nomesDeProfissional.get(faixa.professionalId()),
                        faixa.dayOfWeek(),
                        faixa.startsAt(),
                        faixa.endsAt()))
                .toList();
    }
}
