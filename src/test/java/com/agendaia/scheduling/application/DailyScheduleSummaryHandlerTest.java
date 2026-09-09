package com.agendaia.scheduling.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.customer.api.CustomerDirectory;
import com.agendaia.customer.api.CustomerRef;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.scheduling.application.port.out.AppointmentRepository;
import com.agendaia.scheduling.domain.Appointment;
import com.agendaia.scheduling.domain.AppointmentStatus;
import com.agendaia.scheduling.domain.PaymentStatus;
import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Sem Spring e sem banco — AppointmentRepository e CustomerDirectory mockados. */
@ExtendWith(MockitoExtension.class)
class DailyScheduleSummaryHandlerTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private CustomerDirectory customerDirectory;

    private DailyScheduleSummaryHandler handler;

    private final TenantId tenant = TenantId.of(UuidV7.generate());
    private final UUID professionalId = UuidV7.generate();
    private final UUID serviceOfferingId = UuidV7.generate();
    private final UUID customerId = UuidV7.generate();
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void montar() {
        handler = new DailyScheduleSummaryHandler(appointmentRepository, customerDirectory);
        TenantContext.set(tenant);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private Appointment agendamento(AppointmentStatus status, Instant startsAt, long precoCentavos) {
        return Appointment.reconstitute(
                UuidV7.generate(),
                tenant,
                professionalId,
                serviceOfferingId,
                customerId,
                status,
                startsAt,
                startsAt.plus(30, ChronoUnit.MINUTES),
                "Corte de Cabelo",
                30,
                new Money(precoCentavos),
                PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("AC-1/AC-3: total e receita contam SCHEDULED/CONFIRMED/COMPLETED, excluem CANCELLED/NO_SHOW")
    void totalERecitaExcluemCanceladoEFalta() {
        var futuro = Instant.now().plus(1, ChronoUnit.HOURS);
        when(appointmentRepository.findByTenantIdAndDate(tenant, hoje))
                .thenReturn(List.of(
                        agendamento(AppointmentStatus.SCHEDULED, futuro, 3000),
                        agendamento(AppointmentStatus.CONFIRMED, futuro.plus(1, ChronoUnit.HOURS), 4000),
                        agendamento(AppointmentStatus.CANCELLED, futuro.plus(2, ChronoUnit.HOURS), 5000),
                        agendamento(AppointmentStatus.NO_SHOW, futuro.plus(3, ChronoUnit.HOURS), 6000)));
        when(customerDirectory.find(customerId)).thenReturn(Optional.of(new CustomerRef(customerId, "João", "+551199990000")));

        var resumo = handler.summaryFor(hoje);

        assertThat(resumo.totalToday()).isEqualTo(2);
        assertThat(resumo.estimatedRevenue()).isEqualTo(new Money(7000));
    }

    @Test
    @DisplayName("AC-2: completedToday conta so COMPLETED")
    void completedTodayContaSoCompleted() {
        var passado = Instant.now().minus(1, ChronoUnit.HOURS);
        when(appointmentRepository.findByTenantIdAndDate(tenant, hoje))
                .thenReturn(List.of(
                        agendamento(AppointmentStatus.COMPLETED, passado, 3000),
                        agendamento(AppointmentStatus.CONFIRMED, passado, 3000)));

        var resumo = handler.summaryFor(hoje);

        assertThat(resumo.completedToday()).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-4: proximo cliente e o de menor startsAt ainda no futuro, entre SCHEDULED/CONFIRMED")
    void proximoClienteEOMenorStartsAtFuturo() {
        var agora = Instant.now();
        var maisCedo = agora.plus(1, ChronoUnit.HOURS);
        var maisTarde = agora.plus(2, ChronoUnit.HOURS);
        when(appointmentRepository.findByTenantIdAndDate(tenant, hoje))
                .thenReturn(List.of(
                        agendamento(AppointmentStatus.CONFIRMED, maisTarde, 3000),
                        agendamento(AppointmentStatus.SCHEDULED, maisCedo, 3000)));
        when(customerDirectory.find(customerId)).thenReturn(Optional.of(new CustomerRef(customerId, "João", "+551199990000")));

        var resumo = handler.summaryFor(hoje);

        assertThat(resumo.nextClient()).isNotNull();
        assertThat(resumo.nextClient().customerName()).isEqualTo("João");
        assertThat(resumo.nextClient().startsAt()).isEqualTo(maisCedo);
        verify(customerDirectory).find(customerId);
    }

    @Test
    @DisplayName("AC-4: sem agendamento ativo no futuro, proximo cliente e null")
    void proximoClienteNuloQuandoNaoHaMaisNadaHoje() {
        var passado = Instant.now().minus(1, ChronoUnit.HOURS);
        when(appointmentRepository.findByTenantIdAndDate(tenant, hoje))
                .thenReturn(List.of(agendamento(AppointmentStatus.COMPLETED, passado, 3000)));

        var resumo = handler.summaryFor(hoje);

        assertThat(resumo.nextClient()).isNull();
        verify(customerDirectory, never()).find(any());
    }
}
