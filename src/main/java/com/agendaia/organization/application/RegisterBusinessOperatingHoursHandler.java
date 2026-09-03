package com.agendaia.organization.application;

import com.agendaia.organization.application.command.RegisterBusinessOperatingHoursCommand;
import com.agendaia.organization.application.port.in.RegisterBusinessOperatingHoursUseCase;
import com.agendaia.organization.application.port.in.RegisteredBusinessOperatingHours;
import com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository;
import com.agendaia.organization.domain.BusinessOperatingHours;
import com.agendaia.platform.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Declara uma faixa de horário de funcionamento do estabelecimento da sessão.
 *
 * <p>Lê o tenant do {@link TenantContext}, nunca de argumento (DD-1). Sem
 * verificação de duplicata a fazer: várias faixas no mesmo dia são
 * permitidas (BR-1).
 */
@Service
public class RegisterBusinessOperatingHoursHandler implements RegisterBusinessOperatingHoursUseCase {

    private final BusinessOperatingHoursRepository businessOperatingHoursRepository;

    public RegisterBusinessOperatingHoursHandler(BusinessOperatingHoursRepository businessOperatingHoursRepository) {
        this.businessOperatingHoursRepository = businessOperatingHoursRepository;
    }

    @Override
    @Transactional
    public RegisteredBusinessOperatingHours register(RegisterBusinessOperatingHoursCommand command) {
        var tenantId = TenantContext.require();

        var faixa = BusinessOperatingHours.register(tenantId, command.dayOfWeek(), command.opensAt(), command.closesAt());
        businessOperatingHoursRepository.saveAndFlush(faixa);

        return new RegisteredBusinessOperatingHours(faixa.id());
    }
}
