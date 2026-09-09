package com.agendaia.scheduling.application;

import com.agendaia.catalog.api.ServiceOfferingDirectory;
import com.agendaia.customer.api.CustomerDirectory;
import com.agendaia.customer.api.CustomerRef;
import com.agendaia.organization.api.ProfessionalDirectory;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.in.AgendaEntry;
import com.agendaia.scheduling.application.port.in.BookAppointmentCommand;
import com.agendaia.scheduling.application.port.in.BookedAppointment;
import com.agendaia.scheduling.application.port.in.CancelAppointmentByOwnerUseCase;
import com.agendaia.scheduling.application.port.in.CompleteAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.CreateAppointmentManuallyUseCase;
import com.agendaia.scheduling.application.port.in.RescheduleAppointmentCommand;
import com.agendaia.scheduling.application.port.in.RescheduleAppointmentUseCase;
import com.agendaia.scheduling.application.port.in.UpdatePaymentStatusUseCase;
import com.agendaia.scheduling.application.port.in.ViewAgendaUseCase;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.PaymentStatus;
import com.agendaia.scheduling.domain.exception.AppointmentNotFoundException;
import com.agendaia.scheduling.domain.exception.ServiceOfferingNotFoundException;
import com.agendaia.scheduling.domain.exception.SlotUnavailableException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agenda do dono sobre o agendamento (agenda-profissional, TODO-008): ver a
 * agenda do dia, criar manualmente, cancelar e reagendar.
 *
 * <p>Uma classe só implementando as quatro portas — mesmo desvio deliberado
 * de {@code ManageAppointmentHandler} (TODO-007, DD-2): as quatro operações
 * compartilham {@code AppointmentRepository} e resolução por tenant, e são
 * a mesma funcionalidade ("o dono gerencia a própria agenda") vista de
 * ângulos diferentes.
 */
@Service
public class ProfessionalAgendaHandler
        implements ViewAgendaUseCase,
                CreateAppointmentManuallyUseCase,
                CancelAppointmentByOwnerUseCase,
                RescheduleAppointmentUseCase,
                CompleteAppointmentUseCase,
                UpdatePaymentStatusUseCase {

    private final AppointmentRepository appointmentRepository;
    private final ServiceOfferingDirectory serviceOfferingDirectory;
    private final CustomerDirectory customerDirectory;
    private final ProfessionalDirectory professionalDirectory;
    private final SchedulingMetrics schedulingMetrics;

    public ProfessionalAgendaHandler(
            AppointmentRepository appointmentRepository,
            ServiceOfferingDirectory serviceOfferingDirectory,
            CustomerDirectory customerDirectory,
            ProfessionalDirectory professionalDirectory,
            SchedulingMetrics schedulingMetrics) {
        this.appointmentRepository = appointmentRepository;
        this.serviceOfferingDirectory = serviceOfferingDirectory;
        this.customerDirectory = customerDirectory;
        this.professionalDirectory = professionalDirectory;
        this.schedulingMetrics = schedulingMetrics;
    }

    /**
     * {@code professionalId} de outro tenant devolve lista vazia, nunca
     * lança — mesmo raciocínio de {@code ProfessionalDirectory.find} vazio
     * (BR-5). {@link CustomerDirectory#findByIds} é chamado uma vez só,
     * nunca em laço por agendamento (PATTERNS.md).
     */
    @Override
    @Transactional(readOnly = true)
    public List<AgendaEntry> handle(UUID professionalId, LocalDate date) {
        var tenantId = TenantContext.require();

        if (professionalDirectory.find(professionalId).isEmpty()) {
            return List.of();
        }

        var agendamentos = appointmentRepository.findByTenantIdAndProfessionalIdAndDate(
                tenantId, professionalId, date);

        var idsDeClientes = agendamentos.stream().map(Appointment::customerId).collect(Collectors.toSet());
        var clientesPorId = customerDirectory.findByIds(idsDeClientes).stream()
                .collect(Collectors.toMap(CustomerRef::id, Function.identity()));

        var agora = Instant.now();
        return agendamentos.stream()
                .map(agendamento -> paraAgendaEntry(agendamento, clientesPorId, agora))
                .toList();
    }

    private static AgendaEntry paraAgendaEntry(
            Appointment agendamento, Map<UUID, CustomerRef> clientesPorId, Instant agora) {
        var cliente = clientesPorId.get(agendamento.customerId());
        // Corrigido na TODO-110 (DD-4): a variável antiga só excluía
        // CANCELLED — um agendamento COMPLETED (ou NO_SHOW) mostraria botão
        // de cancelar/reagendar, o que não faz sentido para um status
        // terminal. Com 5 valores possíveis, "aberto" equivale a
        // SCHEDULED/CONFIRMED.
        var estaAberto = agendamento.status() != AppointmentStatus.CANCELLED
                && agendamento.status() != AppointmentStatus.NO_SHOW
                && agendamento.status() != AppointmentStatus.COMPLETED;
        var aindaNoFuturo = agora.isBefore(agendamento.startsAt());

        return new AgendaEntry(
                agendamento.id(),
                cliente == null ? null : cliente.name(),
                cliente == null ? null : cliente.phone(),
                agendamento.serviceName(),
                agendamento.startsAt(),
                agendamento.endsAt(),
                agendamento.status(),
                agendamento.paymentStatus(),
                estaAberto && aindaNoFuturo && agendamento.status() == AppointmentStatus.SCHEDULED,
                estaAberto,
                estaAberto,
                estaAberto && !aindaNoFuturo);
    }

    /** BR-4: sem teto de agendamentos futuros por telefone — só se aplica à reserva pública (TODO-006). */
    @Override
    @Transactional
    public BookedAppointment create(BookAppointmentCommand command) {
        var tenantId = TenantContext.require();

        var oferta = serviceOfferingDirectory
                .find(command.serviceOfferingId())
                .orElseThrow(ServiceOfferingNotFoundException::new);

        var customerId = customerDirectory.findOrCreate(command.customerName(), command.customerPhone());

        Appointment salvo;
        try {
            salvo = AppointmentFactory.buildAndSave(
                    appointmentRepository, tenantId, oferta.professionalId(), customerId, oferta, command.startsAt());
        } catch (SlotUnavailableException e) {
            schedulingMetrics.slotConflict();
            throw e;
        }
        schedulingMetrics.appointmentCreated();

        return paraBookedAppointment(salvo);
    }

    /** BR-2: sem restrição de horário — o dono corrige mesmo depois do atendimento já ter passado. */
    @Override
    @Transactional
    public void cancel(UUID appointmentId) {
        var tenantId = TenantContext.require();
        var antes = appointmentRepository
                .findByTenantIdAndId(tenantId, appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);

        var depois = antes.cancelByOwner();
        if (depois.status() != antes.status()) {
            appointmentRepository.updateStatus(tenantId, antes.id(), depois.status(), Instant.now());
            schedulingMetrics.appointmentCancelled();
        }
    }

    /** BR-1/BR-3: só de SCHEDULED/CONFIRMED, só depois que startsAt já chegou. */
    @Override
    @Transactional
    public void complete(UUID appointmentId) {
        var tenantId = TenantContext.require();
        var antes = appointmentRepository
                .findByTenantIdAndId(tenantId, appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);

        var agora = Instant.now();
        var depois = antes.complete(agora);
        if (depois.status() != antes.status()) {
            appointmentRepository.updateStatus(tenantId, antes.id(), depois.status(), agora);
            schedulingMetrics.appointmentCompleted();
        }
    }

    /**
     * Sem restrição de transição (BR-2, gestao-de-clientes): qualquer valor
     * de pagamento pode virar qualquer outro, a qualquer momento. Grava só
     * quando há mudança real, mesma disciplina de {@link #complete}.
     */
    @Override
    @Transactional
    public void updatePaymentStatus(UUID appointmentId, PaymentStatus status) {
        var tenantId = TenantContext.require();
        var antes = appointmentRepository
                .findByTenantIdAndId(tenantId, appointmentId)
                .orElseThrow(AppointmentNotFoundException::new);

        var depois =
                switch (status) {
                    case PAID -> antes.markPaymentAsPaid();
                    case PENDING -> antes.markPaymentAsPending();
                    case ON_CREDIT -> antes.markPaymentAsOnCredit();
                };
        if (depois.paymentStatus() != antes.paymentStatus()) {
            appointmentRepository.updatePaymentStatus(tenantId, antes.id(), depois.paymentStatus(), Instant.now());
        }
    }

    /**
     * Cria o novo agendamento antes de cancelar o antigo (DD-9): se o novo
     * horário colidir ({@code SlotUnavailableException}), o cliente nunca
     * fica sem nenhum agendamento válido — o antigo permanece intacto.
     */
    @Override
    @Transactional
    public BookedAppointment reschedule(RescheduleAppointmentCommand command) {
        var tenantId = TenantContext.require();

        var antigo = appointmentRepository
                .findByTenantIdAndId(tenantId, command.appointmentId())
                .orElseThrow(AppointmentNotFoundException::new);

        var novaOferta = serviceOfferingDirectory
                .find(command.newServiceOfferingId())
                .orElseThrow(ServiceOfferingNotFoundException::new);

        Appointment novo;
        try {
            novo = AppointmentFactory.buildAndSave(
                    appointmentRepository,
                    tenantId,
                    novaOferta.professionalId(),
                    antigo.customerId(),
                    novaOferta,
                    command.newStartsAt());
        } catch (SlotUnavailableException e) {
            // Reagendar é "mover", não "criar+cancelar" (DD-3 da spec técnica
            // de observabilidade) — só a falha conta, o antigo permanece
            // intacto (DD-9 da spec técnica de agenda-profissional).
            schedulingMetrics.slotConflict();
            throw e;
        }

        appointmentRepository.updateStatus(tenantId, antigo.id(), AppointmentStatus.CANCELLED, Instant.now());

        return paraBookedAppointment(novo);
    }

    private static BookedAppointment paraBookedAppointment(Appointment agendamento) {
        return new BookedAppointment(
                agendamento.id(),
                agendamento.serviceName(),
                agendamento.startsAt().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
}
