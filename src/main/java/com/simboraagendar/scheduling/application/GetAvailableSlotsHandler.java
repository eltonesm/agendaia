package com.simboraagendar.scheduling.application;

import com.simboraagendar.catalog.api.ServiceOfferingDirectory;
import com.simboraagendar.organization.api.AvailabilityDirectory;
import com.simboraagendar.platform.tenant.TenantContext;
import com.simboraagendar.scheduling.application.port.in.GetAvailableSlotsQuery;
import com.simboraagendar.scheduling.application.port.in.GetAvailableSlotsUseCase;
import com.simboraagendar.scheduling.application.port.out.AppointmentRepository;
import com.simboraagendar.scheduling.domain.AvailableSlot;
import com.simboraagendar.scheduling.domain.SlotCalculator;
import com.simboraagendar.scheduling.domain.exception.AvailabilityQueryOutOfRangeException;
import com.simboraagendar.scheduling.domain.exception.ServiceOfferingNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o cálculo de disponibilidade: valida o horizonte (BR-5), resolve
 * a oferta via {@code catalog.api} (BR-7), lê horário/jornada/bloqueio via
 * {@code organization.api}, e delega o cálculo a {@link SlotCalculator}
 * (domínio puro).
 *
 * <p>Agendamentos já ativos ({@code SCHEDULED}/{@code CONFIRMED}) do
 * profissional entram como mais um bloqueio, junto com {@code TimeOff}
 * (achado durante o TASK-006 de pagina-publica-agendamento) — sem isso, a
 * lista de horários livres nunca refletiria uma reserva já feita, e o
 * cliente só descobriria o conflito ao tentar confirmar.
 */
@Service
public class GetAvailableSlotsHandler implements GetAvailableSlotsUseCase {

    private static final int HORIZON_DAYS = 30;

    private final ServiceOfferingDirectory serviceOfferingDirectory;
    private final AvailabilityDirectory availabilityDirectory;
    private final AppointmentRepository appointmentRepository;

    public GetAvailableSlotsHandler(
            ServiceOfferingDirectory serviceOfferingDirectory,
            AvailabilityDirectory availabilityDirectory,
            AppointmentRepository appointmentRepository) {
        this.serviceOfferingDirectory = serviceOfferingDirectory;
        this.availabilityDirectory = availabilityDirectory;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableSlot> handle(GetAvailableSlotsQuery query) {
        return handle(query, LocalDate.now());
    }

    /** Overload testável com "hoje" explícito (DD-7) — mesmo padrão dos {@code register()} das entidades. */
    List<AvailableSlot> handle(GetAvailableSlotsQuery query, LocalDate hoje) {
        validarHorizonte(query.date(), hoje);

        var oferta = serviceOfferingDirectory
                .find(query.serviceOfferingId())
                .orElseThrow(ServiceOfferingNotFoundException::new);

        var dayOfWeek = query.date().getDayOfWeek();
        var businessHours = availabilityDirectory.operatingHoursFor(dayOfWeek);
        var workSchedule = availabilityDirectory.workScheduleFor(oferta.professionalId(), dayOfWeek);

        var blocked = new ArrayList<>(availabilityDirectory.blocksFor(oferta.professionalId(), query.date()));
        blocked.addAll(appointmentRepository.findOccupiedRanges(
                TenantContext.require(), oferta.professionalId(), query.date()));

        var candidatos = SlotCalculator.calculate(
                businessHours, workSchedule, blocked, oferta.durationMinutes(), oferta.bufferMinutes());

        return candidatos.stream()
                .map(faixa -> new AvailableSlot(
                        oferta.professionalId(),
                        oferta.id(),
                        query.date().atTime(faixa.start()),
                        query.date().atTime(faixa.end())))
                .toList();
    }

    private static void validarHorizonte(LocalDate data, LocalDate hoje) {
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(HORIZON_DAYS))) {
            throw new AvailabilityQueryOutOfRangeException();
        }
    }
}
