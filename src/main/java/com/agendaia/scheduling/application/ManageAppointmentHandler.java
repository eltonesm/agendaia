package com.agendaia.scheduling.application;

import com.agendaia.customer.api.CustomerDirectory;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.in.AppointmentDetails;
import com.agendaia.scheduling.application.port.in.AppointmentDetailsUseCase;
import com.agendaia.scheduling.application.port.in.CancelAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.ConfirmAppointmentUseCase;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.exception.AppointmentNotFoundException;
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

    public ManageAppointmentHandler(
            AppointmentRepository appointmentRepository,
            ProfessionalDirectory professionalDirectory,
            CustomerDirectory customerDirectory) {
        this.appointmentRepository = appointmentRepository;
        this.professionalDirectory = professionalDirectory;
        this.customerDirectory = customerDirectory;
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
        gravarSeMudou(resolverOuFalhar(appointmentId), Appointment::cancel);
    }

    private Appointment resolverOuFalhar(UUID appointmentId) {
        var tenantId = TenantContext.require();
        return appointmentRepository
                .findByTenantIdAndId(tenantId, appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);
    }

    private void gravarSeMudou(Appointment antes, BiFunction<Appointment, Instant, Appointment> transicao) {
        var agora = Instant.now();
        var depois = transicao.apply(antes, agora);
        if (depois.status() != antes.status()) {
            appointmentRepository.updateStatus(antes.tenantId(), antes.id(), depois.status(), agora);
        }
    }
}
