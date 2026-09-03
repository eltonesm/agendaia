package com.agendaia.organization.application;

import com.agendaia.organization.application.command.RegisterTimeOffCommand;
import com.agendaia.organization.application.port.in.RegisterTimeOffUseCase;
import com.agendaia.organization.application.port.in.RegisteredTimeOff;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.TimeOffRepository;
import com.agendaia.organization.domain.TimeOff;
import com.agendaia.organization.domain.exception.ProfessionalNotFoundException;
import com.agendaia.platform.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra um bloqueio do estabelecimento da sessão.
 *
 * <p>Lê o tenant do {@link TenantContext}, nunca de argumento (DD-1).
 * {@code professionalId} nulo vale para o estabelecimento inteiro (DD-3) e
 * pula a validação de profissional — só quando informado é que precisa
 * pertencer ao tenant da sessão (BR-8).
 */
@Service
public class RegisterTimeOffHandler implements RegisterTimeOffUseCase {

    private final TimeOffRepository timeOffRepository;
    private final ProfessionalRepository professionalRepository;

    public RegisterTimeOffHandler(TimeOffRepository timeOffRepository, ProfessionalRepository professionalRepository) {
        this.timeOffRepository = timeOffRepository;
        this.professionalRepository = professionalRepository;
    }

    @Override
    @Transactional
    public RegisteredTimeOff register(RegisterTimeOffCommand command) {
        var tenantId = TenantContext.require();

        if (command.professionalId() != null
                && !professionalRepository.existsByIdAndTenantId(command.professionalId(), tenantId.value())) {
            throw new ProfessionalNotFoundException();
        }

        var bloqueio = TimeOff.register(
                tenantId, command.professionalId(), command.startsAt(), command.endsAt(), command.reason());
        timeOffRepository.saveAndFlush(bloqueio);

        return new RegisteredTimeOff(bloqueio.id());
    }
}
