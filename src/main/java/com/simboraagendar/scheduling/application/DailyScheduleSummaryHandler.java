package com.simboraagendar.scheduling.application;

import com.simboraagendar.customer.api.CustomerDirectory;
import com.simboraagendar.customer.api.CustomerRef;
import com.simboraagendar.platform.tenant.TenantContext;
import com.simboraagendar.scheduling.api.DailyScheduleDirectory;
import com.simboraagendar.scheduling.api.DailyScheduleSummary;
import com.simboraagendar.scheduling.api.NextClientRef;
import com.simboraagendar.scheduling.application.port.out.AppointmentRepository;
import com.simboraagendar.scheduling.domain.Appointment;
import com.simboraagendar.scheduling.domain.AppointmentStatus;
import com.simboraagendar.shared.Money;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação de {@link DailyScheduleDirectory} (sistema-de-design-admin,
 * TODO-110, DD-6). Uma única consulta ({@link
 * AppointmentRepository#findByTenantIdAndDate}), os 4 números computados em
 * memória — nunca uma consulta agregada por KPI.
 */
@Service
class DailyScheduleSummaryHandler implements DailyScheduleDirectory {

    private final AppointmentRepository appointmentRepository;
    private final CustomerDirectory customerDirectory;

    DailyScheduleSummaryHandler(AppointmentRepository appointmentRepository, CustomerDirectory customerDirectory) {
        this.appointmentRepository = appointmentRepository;
        this.customerDirectory = customerDirectory;
    }

    @Override
    @Transactional(readOnly = true)
    public DailyScheduleSummary summaryFor(LocalDate date) {
        var tenantId = TenantContext.require();
        var agendamentos = appointmentRepository.findByTenantIdAndDate(tenantId, date);

        var totalToday = agendamentos.stream().filter(DailyScheduleSummaryHandler::contaComoDoDia).count();
        var completedToday = agendamentos.stream()
                .filter(a -> a.status() == AppointmentStatus.COMPLETED)
                .count();
        var estimatedRevenueCents = agendamentos.stream()
                .filter(DailyScheduleSummaryHandler::contaComoDoDia)
                .mapToLong(a -> a.price().cents())
                .sum();

        var agora = Instant.now();
        var proximo = agendamentos.stream()
                .filter(DailyScheduleSummaryHandler::estaAtivo)
                .filter(a -> a.startsAt().isAfter(agora))
                .min(Comparator.comparing(Appointment::startsAt))
                .map(this::paraNextClientRef)
                .orElse(null);

        return new DailyScheduleSummary(totalToday, completedToday, new Money(estimatedRevenueCents), proximo);
    }

    private NextClientRef paraNextClientRef(Appointment agendamento) {
        var nome = customerDirectory.find(agendamento.customerId()).map(CustomerRef::name).orElse(null);
        return new NextClientRef(nome, agendamento.startsAt());
    }

    /** SCHEDULED, CONFIRMED ou COMPLETED — exclui CANCELLED/NO_SHOW da contagem e da receita (AC-1/AC-3 da spec funcional). */
    private static boolean contaComoDoDia(Appointment agendamento) {
        return agendamento.status() == AppointmentStatus.SCHEDULED
                || agendamento.status() == AppointmentStatus.CONFIRMED
                || agendamento.status() == AppointmentStatus.COMPLETED;
    }

    /** SCHEDULED ou CONFIRMED — só esses ainda podem virar o "próximo cliente" (AC-2). */
    private static boolean estaAtivo(Appointment agendamento) {
        return agendamento.status() == AppointmentStatus.SCHEDULED || agendamento.status() == AppointmentStatus.CONFIRMED;
    }
}
