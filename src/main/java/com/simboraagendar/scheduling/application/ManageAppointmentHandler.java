package com.simboraagendar.scheduling.application;

import com.simboraagendar.customer.api.CustomerDirectory;
import com.simboraagendar.organization.api.ProfessionalDirectory;
import com.simboraagendar.platform.tenant.TenantContext;
import com.simboraagendar.scheduling.application.port.in.AppointmentDetails;
import com.simboraagendar.scheduling.application.port.in.AppointmentDetailsUseCase;
import com.simboraagendar.scheduling.application.port.in.CancelAppointmentUseCase;
import com.simboraagendar.scheduling.application.port.in.ConfirmAppointmentUseCase;
import com.simboraagendar.scheduling.application.port.out.AppointmentRepository;
import com.simboraagendar.scheduling.domain.Appointment;
import com.simboraagendar.scheduling.domain.AppointmentStatus;
import com.simboraagendar.scheduling.domain.exception.AppointmentNotFoundException;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gerencia o agendamento já criado, pelo link (confirmacao-e-cancelamento,
 * TODO-007) — consultar, confirmar presença e cancelar.
 *
 * <p>Uma classe só implementando as três portas (DD-2 da spec técnica),
 * desvio deliberado do precedente "uma classe por porta"
 * ({@code BookAppointmentHandler}, {@code GetAvailableSlotsHandler}): as
 * três operações compartilham a mesma dependência
 * ({@code AppointmentRepository}) e a mesma resolução por tenant+id — ao
 * contrário daqueles dois, que são etapas de fluxos diferentes (reservar
 * vs. consultar disponibilidade antes de reservar), estas três são a mesma
 * funcionalidade ("gerenciar o agendamento existente") vista de três
 * ângulos.
 */
@Service
public class ManageAppointmentHandler
        implements AppointmentDetailsUseCase, ConfirmAppointmentUseCase, CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ProfessionalDirectory professionalDirectory;
    private final CustomerDirectory customerDirectory;
    private final SchedulingMetrics schedulingMetrics;

    public ManageAppointmentHandler(
            AppointmentRepository appointmentRepository,
            ProfessionalDirectory professionalDirectory,
            CustomerDirectory customerDirectory,
            SchedulingMetrics schedulingMetrics) {
        this.appointmentRepository = appointmentRepository;
        this.professionalDirectory = professionalDirectory;
        this.customerDirectory = customerDirectory;
        this.schedulingMetrics = schedulingMetrics;
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentDetails handle(UUID appointmentId) {
        var appointment = resolverOuFalhar(appointmentId);

        var professional = professionalDirectory
                .find(appointment.professionalId())
                .orElseThrow(AppointmentNotFoundException::new);
        var customer = customerDirectory.find(appointment.customerId()).orElseThrow(AppointmentNotFoundException::new);

        var agora = Instant.now();
        var aindaNoFuturo = agora.isBefore(appointment.startsAt());
        var naoCancelado = appointment.status() != AppointmentStatus.CANCELLED;
        var podeAgir = naoCancelado && aindaNoFuturo;

        return new AppointmentDetails(
                appointment.id(),
                professional.name(),
                customer.name(),
                appointment.serviceName(),
                appointment.startsAt(),
                appointment.endsAt(),
                appointment.status(),
                podeAgir && appointment.status() == AppointmentStatus.SCHEDULED,
                podeAgir);
    }

    @Override
    @Transactional
    public void confirm(UUID appointmentId) {
        gravarSeMudou(resolverOuFalhar(appointmentId), Appointment::confirm);
    }

    @Override
    @Transactional
    public void cancel(UUID appointmentId) {
        if (gravarSeMudou(resolverOuFalhar(appointmentId), Appointment::cancel)) {
            schedulingMetrics.appointmentCancelled();
        }
    }

    private Appointment resolverOuFalhar(UUID appointmentId) {
        var tenantId = TenantContext.require();
        return appointmentRepository
                .findByTenantIdAndId(tenantId, appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
    }

    /** @return {@code true} se houve transição real (e portanto gravação). */
    private boolean gravarSeMudou(Appointment antes, BiFunction<Appointment, Instant, Appointment> transicao) {
        var agora = Instant.now();
        var depois = transicao.apply(antes, agora);
        if (depois.status() == antes.status()) {
            return false;
        }
        appointmentRepository.updateStatus(antes.tenantId(), antes.id(), depois.status(), agora);
        return true;
    }
}
