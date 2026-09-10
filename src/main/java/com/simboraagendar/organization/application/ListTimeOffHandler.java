package com.simboraagendar.organization.application;

import com.simboraagendar.organization.application.port.in.ListTimeOffUseCase;
import com.simboraagendar.organization.application.port.in.TimeOffView;
import com.simboraagendar.organization.application.port.out.ProfessionalRepository;
import com.simboraagendar.organization.application.port.out.TimeOffRepository;
import com.simboraagendar.organization.domain.Professional;
import com.simboraagendar.organization.domain.TimeOff;
import com.simboraagendar.platform.tenant.TenantContext;
import java.util.List;
import java.util.Objects;
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
                .filter(Objects::nonNull)
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
