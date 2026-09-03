package com.agendaia.organization.application;

import com.agendaia.organization.application.command.RegisterWorkScheduleCommand;
import com.agendaia.organization.application.port.in.RegisterWorkScheduleUseCase;
import com.agendaia.organization.application.port.in.RegisteredWorkSchedule;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.WorkScheduleRepository;
import com.agendaia.organization.domain.WorkSchedule;
import com.agendaia.organization.domain.exception.ProfessionalNotFoundException;
import com.agendaia.organization.domain.exception.WorkScheduleOverlapException;
import com.agendaia.platform.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Declara uma faixa de jornada de um profissional do estabelecimento da
 * sessão.
 *
 * <p>Lê o tenant do {@link TenantContext}, nunca de argumento (DD-1). Valida
 * o profissional contra o tenant (BR-8) e recusa faixa sobreposta a outra do
 * mesmo profissional no mesmo dia (BR-3, DD-2) — em memória, não por
 * exclusion constraint: ver DD-2 da spec técnica para o porquê.
 */
@Service
public class RegisterWorkScheduleHandler implements RegisterWorkScheduleUseCase {

    private final WorkScheduleRepository workScheduleRepository;
    private final ProfessionalRepository professionalRepository;

    public RegisterWorkScheduleHandler(
            WorkScheduleRepository workScheduleRepository, ProfessionalRepository professionalRepository) {
        this.workScheduleRepository = workScheduleRepository;
        this.professionalRepository = professionalRepository;
    }

    @Override
    @Transactional
    public RegisteredWorkSchedule register(RegisterWorkScheduleCommand command) {
        var tenantId = TenantContext.require();

        if (!professionalRepository.existsByIdAndTenantId(command.professionalId(), tenantId.value())) {
            throw new ProfessionalNotFoundException();
        }

        var faixa = WorkSchedule.register(
                tenantId, command.professionalId(), command.dayOfWeek(), command.startsAt(), command.endsAt());

        var faixasDoMesmoDia = workScheduleRepository.findByTenantIdAndProfessionalIdAndDayOfWeekAndActiveTrue(
                tenantId.value(), command.professionalId(), command.dayOfWeek());
        var sobrepoe = faixasDoMesmoDia.stream().anyMatch(faixa::overlaps);
        if (sobrepoe) {
            throw new WorkScheduleOverlapException();
        }

        workScheduleRepository.saveAndFlush(faixa);

        return new RegisteredWorkSchedule(faixa.id());
    }
}
