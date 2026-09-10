package com.simboraagendar.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.simboraagendar.shared.Money;
import com.simboraagendar.shared.TenantId;
import com.simboraagendar.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring e sem banco. */
class AppointmentTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());
    private static final Instant STARTS_AT = Instant.parse("2026-09-07T11:00:00Z");
    private static final Instant ENDS_AT = STARTS_AT.plus(30, ChronoUnit.MINUTES);

    private static Appointment agendamentoValido() {
        return Appointment.schedule(
                TENANT,
                UuidV7.generate(),
                UuidV7.generate(),
                UuidV7.generate(),
                "Corte de Cabelo",
                30,
                new Money(3000),
                STARTS_AT,
                ENDS_AT);
    }

    @Test
    @DisplayName("nasce sempre SCHEDULED, com id UUIDv7 (BR-1)")
    void nasceSempreScheduled() {
        var agendamento = agendamentoValido();

        assertThat(agendamento.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(agendamento.id()).isNotNull();
        assertThat(agendamento.id().version()).isEqualTo(7);
    }

    @Test
    @DisplayName("nasce sempre com status de pagamento PENDING (gestao-de-clientes, BR-3)")
    void nasceSemprePendente() {
        assertThat(agendamentoValido().paymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("markPaymentAsPaid() transiciona de qualquer valor para PAID (BR-2)")
    void markPaymentAsPaidTransiciona() {
        assertThat(reconstituirCom(AppointmentStatus.SCHEDULED, PaymentStatus.PENDING)
                        .markPaymentAsPaid()
                        .paymentStatus())
                .isEqualTo(PaymentStatus.PAID);
        assertThat(reconstituirCom(AppointmentStatus.COMPLETED, PaymentStatus.ON_CREDIT)
                        .markPaymentAsPaid()
                        .paymentStatus())
                .isEqualTo(PaymentStatus.PAID);
    }

    @Test
    @DisplayName("markPaymentAsPending() transiciona de qualquer valor para PENDING (BR-2)")
    void markPaymentAsPendingTransiciona() {
        assertThat(reconstituirCom(AppointmentStatus.COMPLETED, PaymentStatus.PAID)
                        .markPaymentAsPending()
                        .paymentStatus())
                .isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("markPaymentAsOnCredit() transiciona de qualquer valor para ON_CREDIT (BR-2)")
    void markPaymentAsOnCreditTransiciona() {
        assertThat(reconstituirCom(AppointmentStatus.COMPLETED, PaymentStatus.PAID)
                        .markPaymentAsOnCredit()
                        .paymentStatus())
                .isEqualTo(PaymentStatus.ON_CREDIT);
    }

    @Test
    @DisplayName("marcar o mesmo status de pagamento absorve — sem transição (idempotência)")
    void marcarMesmoStatusAbsorve() {
        var pago = reconstituirCom(AppointmentStatus.COMPLETED, PaymentStatus.PAID);
        var pendente = reconstituirCom(AppointmentStatus.SCHEDULED, PaymentStatus.PENDING);
        var fiado = reconstituirCom(AppointmentStatus.COMPLETED, PaymentStatus.ON_CREDIT);

        assertThat(pago.markPaymentAsPaid()).isSameAs(pago);
        assertThat(pendente.markPaymentAsPending()).isSameAs(pendente);
        assertThat(fiado.markPaymentAsOnCredit()).isSameAs(fiado);
    }

    @Test
    @DisplayName("status de pagamento é independente do status do agendamento (BR-2)")
    void statusDePagamentoNaoAfetaStatusDoAgendamento() {
        var cancelado = reconstituirCom(AppointmentStatus.CANCELLED, PaymentStatus.PENDING);

        var depois = cancelado.markPaymentAsOnCredit();

        assertThat(depois.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(depois.paymentStatus()).isEqualTo(PaymentStatus.ON_CREDIT);
    }

    @Test
    @DisplayName("guarda o retrato de serviço/duração/preço informado, não uma referência viva (BR-2)")
    void guardaORetrato() {
        var agendamento = agendamentoValido();

        assertThat(agendamento.serviceName()).isEqualTo("Corte de Cabelo");
        assertThat(agendamento.durationMinutes()).isEqualTo(30);
        assertThat(agendamento.price()).isEqualTo(new Money(3000));
        assertThat(agendamento.startsAt()).isEqualTo(STARTS_AT);
        assertThat(agendamento.endsAt()).isEqualTo(ENDS_AT);
    }

    @Test
    @DisplayName("recusa nascer sem estabelecimento")
    void recusaSemTenant() {
        assertThatThrownBy(() -> Appointment.schedule(
                        null,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    @DisplayName("recusa nascer sem profissional, oferta ou cliente")
    void recusaSemProfissionalOfertaOuCliente() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        null,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        null,
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        null,
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("recusa nascer sem o nome do serviço")
    void recusaSemNomeDoServico() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "  ",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nome do serviço");
    }

    @Test
    @DisplayName("recusa duração zero ou negativa")
    void recusaDuracaoInvalida() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        0,
                        new Money(3000),
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duração");
    }

    @Test
    @DisplayName("recusa nascer sem preço")
    void recusaSemPreco() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        null,
                        STARTS_AT,
                        ENDS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preço");
    }

    @Test
    @DisplayName("recusa fim antes ou igual ao início")
    void recusaFimAntesOuIgualAoInicio() {
        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        STARTS_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("início e fim");

        assertThatThrownBy(() -> Appointment.schedule(
                        TENANT,
                        UuidV7.generate(),
                        UuidV7.generate(),
                        UuidV7.generate(),
                        "Corte de Cabelo",
                        30,
                        new Money(3000),
                        STARTS_AT,
                        STARTS_AT.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("igualdade por identidade")
    void igualdadePorId() {
        var agendamento = agendamentoValido();

        assertThat(agendamento).isEqualTo(agendamento);
        assertThat(agendamento).isNotEqualTo(agendamentoValido());
    }

    @Test
    @DisplayName("reconstitute preserva o status persistido, não força SCHEDULED")
    void reconstitutePreservaOStatus() {
        var id = UuidV7.generate();
        var agendamento = Appointment.reconstitute(
                id,
                TENANT,
                UuidV7.generate(),
                UuidV7.generate(),
                UuidV7.generate(),
                AppointmentStatus.CANCELLED,
                STARTS_AT,
                ENDS_AT,
                "Corte de Cabelo",
                30,
                new Money(3000),
                PaymentStatus.ON_CREDIT);

        assertThat(agendamento.id()).isEqualTo(id);
        assertThat(agendamento.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(agendamento.paymentStatus()).isEqualTo(PaymentStatus.ON_CREDIT);
    }

    private static Appointment reconstituirCom(AppointmentStatus status) {
        return reconstituirCom(status, PaymentStatus.PENDING);
    }

    private static Appointment reconstituirCom(AppointmentStatus status, PaymentStatus paymentStatus) {
        return Appointment.reconstitute(
                UuidV7.generate(),
                TENANT,
                UuidV7.generate(),
                UuidV7.generate(),
                UuidV7.generate(),
                status,
                STARTS_AT,
                ENDS_AT,
                "Corte de Cabelo",
                30,
                new Money(3000),
                paymentStatus);
    }

    @Test
    @DisplayName("confirm() muda SCHEDULED para CONFIRMED quando agora está antes de startsAt (AC-1)")
    void confirmMudaScheduledParaConfirmed() {
        var agendamento = agendamentoValido();

        var confirmado = agendamento.confirm(STARTS_AT.minusSeconds(60));

        assertThat(confirmado.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(confirmado).isNotSameAs(agendamento);
    }

    @Test
    @DisplayName("confirm() e cancel() são no-op quando já CANCELLED (AC-2)")
    void confirmECancelSaoNoOpQuandoJaCancelado() {
        var cancelado = reconstituirCom(AppointmentStatus.CANCELLED);
        var antesDoHorario = STARTS_AT.minusSeconds(60);

        assertThat(cancelado.confirm(antesDoHorario)).isSameAs(cancelado);
        assertThat(cancelado.cancel(antesDoHorario)).isSameAs(cancelado);
    }

    @Test
    @DisplayName("confirm() é no-op quando já CONFIRMED — idempotência (AC-3)")
    void confirmEIdempotente() {
        var confirmado = reconstituirCom(AppointmentStatus.CONFIRMED);

        assertThat(confirmado.confirm(STARTS_AT.minusSeconds(60))).isSameAs(confirmado);
    }

    @Test
    @DisplayName("confirm() e cancel() são no-op quando agora já passou de startsAt (AC-4)")
    void semEfeitoQuandoHorarioJaPassou() {
        var scheduled = agendamentoValido();
        var confirmed = reconstituirCom(AppointmentStatus.CONFIRMED);
        var depoisDoHorario = ENDS_AT.plusSeconds(60);

        assertThat(scheduled.confirm(depoisDoHorario)).isSameAs(scheduled);
        assertThat(scheduled.cancel(depoisDoHorario)).isSameAs(scheduled);
        assertThat(confirmed.cancel(depoisDoHorario)).isSameAs(confirmed);
    }

    @Test
    @DisplayName("cancel() muda SCHEDULED ou CONFIRMED para CANCELLED quando agora está antes de startsAt (AC-5)")
    void cancelMudaParaCancelled() {
        var scheduled = agendamentoValido();
        var confirmed = reconstituirCom(AppointmentStatus.CONFIRMED);
        var antesDoHorario = STARTS_AT.minusSeconds(60);

        assertThat(scheduled.cancel(antesDoHorario).status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(confirmed.cancel(antesDoHorario).status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelByOwner() muda para CANCELLED mesmo com startsAt no passado (BR-2, agenda-profissional)")
    void cancelByOwnerIgnoraHorario() {
        var scheduled = agendamentoValido();
        var confirmed = reconstituirCom(AppointmentStatus.CONFIRMED);

        assertThat(scheduled.cancelByOwner().status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(confirmed.cancelByOwner().status()).isEqualTo(AppointmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelByOwner() é no-op quando já CANCELLED — idempotência")
    void cancelByOwnerEIdempotente() {
        var cancelado = reconstituirCom(AppointmentStatus.CANCELLED);

        assertThat(cancelado.cancelByOwner()).isSameAs(cancelado);
    }

    @Test
    @DisplayName("cancelByOwner() nao reabre um COMPLETED — status terminal, sem volta (BR-2, sistema-de-design-admin)")
    void cancelByOwnerNaoReabreCompleted() {
        var concluido = reconstituirCom(AppointmentStatus.COMPLETED);

        assertThat(concluido.cancelByOwner()).isSameAs(concluido);
    }

    @Test
    @DisplayName("complete() muda SCHEDULED ou CONFIRMED para COMPLETED quando agora já alcançou startsAt (BR-1/BR-3)")
    void completeMudaParaCompleted() {
        var scheduled = agendamentoValido();
        var confirmed = reconstituirCom(AppointmentStatus.CONFIRMED);
        var depoisDoInicio = STARTS_AT.plusSeconds(60);

        assertThat(scheduled.complete(depoisDoInicio).status()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(confirmed.complete(depoisDoInicio).status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("complete() é no-op antes de startsAt — não dá para concluir o que ainda não começou (BR-3)")
    void completeENoOpAntesDoInicio() {
        var scheduled = agendamentoValido();
        var antesDoInicio = STARTS_AT.minusSeconds(60);

        assertThat(scheduled.complete(antesDoInicio)).isSameAs(scheduled);
    }

    @Test
    @DisplayName("complete() é no-op para CANCELLED e NO_SHOW (BR-1)")
    void completeENoOpParaCancelledENoShow() {
        var cancelado = reconstituirCom(AppointmentStatus.CANCELLED);
        var faltou = reconstituirCom(AppointmentStatus.NO_SHOW);
        var depoisDoInicio = STARTS_AT.plusSeconds(60);

        assertThat(cancelado.complete(depoisDoInicio)).isSameAs(cancelado);
        assertThat(faltou.complete(depoisDoInicio)).isSameAs(faltou);
    }

    @Test
    @DisplayName("complete() é no-op quando já COMPLETED — idempotência")
    void completeEIdempotente() {
        var concluido = reconstituirCom(AppointmentStatus.COMPLETED);

        assertThat(concluido.complete(STARTS_AT.plusSeconds(60))).isSameAs(concluido);
    }
}
