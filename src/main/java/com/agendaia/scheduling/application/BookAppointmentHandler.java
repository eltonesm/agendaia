package com.agendaia.scheduling.application;

import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.customer.api.CustomerDirectory;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.in.BookAppointmentCommand;
import com.agendaia.scheduling.application.port.in.BookAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.BookedAppointment;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.exception.PhoneAppointmentLimitExceededException;
import com.agendaia.scheduling.domain.exception.ServiceOfferingNotFoundException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra a reserva (US-4/US-6): revalida a oferta contra o tenant
 * (BR-5), resolve/cria o {@code Customer} (BR-3, DD-5 da spec técnica),
 * checa o teto por telefone (BR-9), monta o retrato (BR-2) e grava — a
 * garantia real contra overbooking é a exclusion constraint do banco
 * (BR-4/ADR 0005), traduzida em {@code SlotUnavailableException} pelo
 * adapter de persistência.
 */
@Service
public class BookAppointmentHandler implements BookAppointmentUseCase {

    /** BR-9: teto de agendamentos futuros ativos por telefone, dentro do mesmo tenant. */
    static final int MAX_FUTURE_APPOINTMENTS_PER_PHONE = 3;

    private final ServiceOfferingDirectory serviceOfferingDirectory;
    private final CustomerDirectory customerDirectory;
    private final AppointmentRepository appointmentRepository;

    public BookAppointmentHandler(
            ServiceOfferingDirectory serviceOfferingDirectory,
            CustomerDirectory customerDirectory,
            AppointmentRepository appointmentRepository) {
        this.serviceOfferingDirectory = serviceOfferingDirectory;
        this.customerDirectory = customerDirectory;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    @Transactional
    public BookedAppointment handle(BookAppointmentCommand command) {
        var tenantId = TenantContext.require();

        var oferta = serviceOfferingDirectory
                .find(command.serviceOfferingId())
                .orElseThrow(ServiceOfferingNotFoundException::new);

        // DD-5: get-or-create de Customer dentro da mesma transação que grava
        // o Appointment — mesmo padrão de BillingAccountService.criarConta
        // (back-office-operador), que já cruza organization.api assim.
        var customerId = customerDirectory.findOrCreate(command.customerName(), command.customerPhone());

        if (appointmentRepository.countFutureActive(tenantId, customerId, Instant.now())
                >= MAX_FUTURE_APPOINTMENTS_PER_PHONE) {
            throw new PhoneAppointmentLimitExceededException();
        }

        var endsAt = command.startsAt().plus(oferta.durationMinutes(), ChronoUnit.MINUTES);

        var agendamento = Appointment.schedule(
                tenantId,
                oferta.professionalId(),
                oferta.id(),
                customerId,
                oferta.serviceName(),
                oferta.durationMinutes(),
                oferta.price(),
                command.startsAt(),
                endsAt);

        var salvo = appointmentRepository.save(agendamento);

        return new BookedAppointment(
                salvo.id(), salvo.serviceName(), salvo.startsAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
}
