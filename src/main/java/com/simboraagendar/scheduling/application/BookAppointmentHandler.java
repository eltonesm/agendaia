package com.simboraagendar.scheduling.application;

import com.simboraagendar.catalog.api.ServiceOfferingDirectory;
import com.simboraagendar.customer.api.CustomerDirectory;
import com.simboraagendar.platform.tenant.TenantContext;
import com.simboraagendar.scheduling.application.port.in.BookAppointmentCommand;
import com.simboraagendar.scheduling.application.port.in.BookAppointmentUseCase;
import com.simboraagendar.scheduling.application.port.in.BookedAppointment;
import com.simboraagendar.scheduling.application.port.out.AppointmentRepository;
import com.simboraagendar.scheduling.domain.Appointment;
import com.simboraagendar.scheduling.domain.exception.PhoneAppointmentLimitExceededException;
import com.simboraagendar.scheduling.domain.exception.ServiceOfferingNotFoundException;
import com.simboraagendar.scheduling.domain.exception.SlotUnavailableException;
import java.time.Instant;
import java.time.ZoneId;
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
    private final SchedulingMetrics schedulingMetrics;

    public BookAppointmentHandler(
            ServiceOfferingDirectory serviceOfferingDirectory,
            CustomerDirectory customerDirectory,
            AppointmentRepository appointmentRepository,
            SchedulingMetrics schedulingMetrics) {
        this.serviceOfferingDirectory = serviceOfferingDirectory;
        this.customerDirectory = customerDirectory;
        this.appointmentRepository = appointmentRepository;
        this.schedulingMetrics = schedulingMetrics;
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

        Appointment salvo;
        try {
            salvo = AppointmentFactory.buildAndSave(
                    appointmentRepository, tenantId, oferta.professionalId(), customerId, oferta, command.startsAt());
        } catch (SlotUnavailableException e) {
            schedulingMetrics.slotConflict();
            throw e;
        }
        schedulingMetrics.appointmentCreated();

        return new BookedAppointment(
                salvo.id(), salvo.serviceName(), salvo.startsAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
}
