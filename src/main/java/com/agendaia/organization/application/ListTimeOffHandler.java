package com.agendaia.organization.application;

import com.agendaia.organization.application.port.in.ListTimeOffUseCase;
import com.agendaia.organization.application.port.in.TimeOffView;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.TimeOffRepository;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.TimeOff;
import com.agendaia.platform.tenant.TenantContext;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lista os bloqueios do estabelecimento da sessão, com o nome do profissional
 * já resolvido quando presente.
 *
 * <p>Tenant lido do {@link TenantContext}, nunca de argumento (DD-1).
 */
@Service
public class ListTimeOffHandler implements ListTimeOffUseCase {

    private final TimeOffRepository timeOffRepository;
    private final ProfessionalRepository professionalRepository;

    public ListTimeOffHandler(TimeOffRepository timeOffRepository, ProfessionalRepository professionalRepository) {
        this.timeOffRepository = timeOffRepository;
        this.professionalRepository = professionalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeOffView> list() {
        var tenantId = TenantContext.require();

        var bloqueios = timeOffRepository.findByTenantIdAndActiveTrueOrderByStartsAtDesc(tenantId.value());

        var idsDeProfissional = bloqueios.stream()
                .map(TimeOff::professionalId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        var nomesDeProfissional = professionalRepository.findAllById(idsDeProfissional).stream()
                .collect(Collectors.toMap(Professional::id, Professional::name));

        return bloqueios.stream()
                .map(bloqueio -> new TimeOffView(
                        bloqueio.id(),
                        nomesDeProfissional.get(bloqueio.professionalId()),
                        bloqueio.startsAt(),
                        bloqueio.endsAt(),
                        bloqueio.reason()))
                .toList();
    }
}
