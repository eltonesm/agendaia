package com.simboraagendar.scheduling.domain;

import com.simboraagendar.shared.Money;
import com.simboraagendar.shared.TenantId;
import com.simboraagendar.shared.UuidV7;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Um atendimento marcado (glossário). Guarda o <strong>retrato</strong> de
 * duração e preço no momento da reserva (BR-2 da spec funcional de
 * pagina-publica-agendamento) — mudança futura no catálogo não afeta
 * agendamentos já criados.
 *
 * <p>Java puro, regime completo (ADR 0002) — {@code scheduling} é o core
 * domain. A garantia real contra overbooking é a exclusion constraint do
 * banco (ADR 0005); {@link #schedule} só valida forma, nunca sobreposição
 * de horário.
 */
public final class Appointment {

    private final UUID id;
    private final UUID tenantId;
    private final UUID professionalId;
    private final UUID serviceOfferingId;
    private final UUID customerId;
    private final AppointmentStatus status;
    private final Instant startsAt;
    private final Instant endsAt;
    private final String serviceName;
    private final int durationMinutes;
    private final Money price;
    private final PaymentStatus paymentStatus;

    private Appointment(
            UUID id,
            UUID tenantId,
            UUID professionalId,
            UUID serviceOfferingId,
            UUID customerId,
            AppointmentStatus status,
            Instant startsAt,
            Instant endsAt,
            String serviceName,
            int durationMinutes,
            Money price,
            PaymentStatus paymentStatus) {
        this.id = id;
        this.tenantId = tenantId;
        this.professionalId = professionalId;
        this.serviceOfferingId = serviceOfferingId;
        this.customerId = customerId;
        this.status = status;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.serviceName = serviceName;
        this.durationMinutes = durationMinutes;
        this.price = price;
        this.paymentStatus = paymentStatus;
    }

    /** Nasce sempre {@link AppointmentStatus#SCHEDULED} (BR-1). */
    public static Appointment schedule(
            TenantId tenantId,
            UUID professionalId,
            UUID serviceOfferingId,
            UUID customerId,
            String serviceName,
            int durationMinutes,
            Money price,
            Instant startsAt,
            Instant endsAt) {
        if (tenantId == null) {
            throw new IllegalArgumentException("agendamento não existe sem estabelecimento");
        }
        if (professionalId == null || serviceOfferingId == null || customerId == null) {
            throw new IllegalArgumentException("agendamento precisa de profissional, oferta e cliente");
        }
        var nomeLimpo = serviceName == null ? "" : serviceName.strip();
        if (nomeLimpo.isEmpty()) {
            throw new IllegalArgumentException("agendamento precisa do nome do serviço no momento da reserva");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("duração do agendamento precisa ser positiva");
        }
        if (price == null) {
            throw new IllegalArgumentException("agendamento precisa de um preço, mesmo que zero");
        }
        if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("agendamento precisa de início e fim válidos");
        }
        return new Appointment(
                UuidV7.generate(),
                tenantId.value(),
                professionalId,
                serviceOfferingId,
                customerId,
                AppointmentStatus.SCHEDULED,
                startsAt,
                endsAt,
                nomeLimpo,
                durationMinutes,
                price,
                PaymentStatus.PENDING);
    }

    /** Reconstrói a partir de dado persistido — usado só pelo mapper (adapter.out.persistence). */
    public static Appointment reconstitute(
            UUID id,
            TenantId tenantId,
            UUID professionalId,
            UUID serviceOfferingId,
            UUID customerId,
            AppointmentStatus status,
            Instant startsAt,
            Instant endsAt,
            String serviceName,
            int durationMinutes,
            Money price,
            PaymentStatus paymentStatus) {
        return new Appointment(
                id,
                tenantId.value(),
                professionalId,
                serviceOfferingId,
                customerId,
                status,
                startsAt,
                endsAt,
                serviceName,
                durationMinutes,
                price,
                paymentStatus);
    }

    public UUID id() {
        return id;
    }

    public TenantId tenantId() {
        return TenantId.of(tenantId);
    }

    public UUID professionalId() {
        return professionalId;
    }

    public UUID serviceOfferingId() {
        return serviceOfferingId;
    }

    public UUID customerId() {
        return customerId;
    }

    public AppointmentStatus status() {
        return status;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public Instant endsAt() {
        return endsAt;
    }

    public String serviceName() {
        return serviceName;
    }

    public int durationMinutes() {
        return durationMinutes;
    }

    public Money price() {
        return price;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    /**
     * {@code SCHEDULED} → {@code CONFIRMED} (US-2, confirmacao-e-cancelamento,
     * TODO-007). Absorve estado terminal sem lançar exceção — devolve a
     * própria instância, sem transição, quando já está {@code CANCELLED}
     * (BR-3), quando {@code agora} já passou de {@code startsAt} (BR-4), ou
     * quando já está {@code CONFIRMED} (idempotência, BR-3). Quem chama
     * compara {@code antes.status() != depois.status()} para saber se há
     * gravação a fazer.
     */
    public Appointment confirm(Instant agora) {
        if (status == AppointmentStatus.CANCELLED || agora.isAfter(startsAt) || status == AppointmentStatus.CONFIRMED) {
            return this;
        }
        return new Appointment(
                id, tenantId, professionalId, serviceOfferingId, customerId,
                AppointmentStatus.CONFIRMED, startsAt, endsAt, serviceName, durationMinutes, price, paymentStatus);
    }

    /**
     * {@code SCHEDULED}/{@code CONFIRMED} → {@code CANCELLED} (US-3,
     * confirmacao-e-cancelamento, TODO-007). Mesma filosofia de absorção de
     * {@link #confirm}: já {@code CANCELLED} (idempotência) ou {@code agora}
     * depois de {@code startsAt} (BR-4) devolvem a própria instância, sem
     * gravação.
     */
    public Appointment cancel(Instant agora) {
        if (status == AppointmentStatus.CANCELLED || agora.isAfter(startsAt)) {
            return this;
        }
        return new Appointment(
                id, tenantId, professionalId, serviceOfferingId, customerId,
                AppointmentStatus.CANCELLED, startsAt, endsAt, serviceName, durationMinutes, price, paymentStatus);
    }

    /**
     * {@code SCHEDULED}/{@code CONFIRMED} → {@code CANCELLED} pelo dono, pelo
     * painel administrativo (US-3, agenda-profissional, TODO-008). Ao
     * contrário de {@link #cancel(Instant)}, não tem restrição de horário
     * (BR-2 — o dono corrige erro de digitação ou desiste em nome do cliente
     * mesmo depois do horário já ter passado). Idempotente se já
     * {@code CANCELLED}; absorve sem transição também se já {@code
     * COMPLETED} (BR-2 de sistema-de-design-admin, TODO-110: {@code
     * COMPLETED} é terminal, sem volta — {@link #cancel(Instant)} e {@link
     * #confirm(Instant)} já ganham essa proteção de graça pela checagem de
     * horário, que {@code cancelByOwner} deliberadamente não tem).
     */
    public Appointment cancelByOwner() {
        if (status == AppointmentStatus.CANCELLED || status == AppointmentStatus.COMPLETED) {
            return this;
        }
        return new Appointment(
                id, tenantId, professionalId, serviceOfferingId, customerId,
                AppointmentStatus.CANCELLED, startsAt, endsAt, serviceName, durationMinutes, price, paymentStatus);
    }

    /**
     * {@code SCHEDULED}/{@code CONFIRMED} → {@code COMPLETED}, pelo dono ou
     * profissional, depois do atendimento (US-4, sistema-de-design-admin,
     * TODO-110). Absorve sem transição — mesma filosofia de {@link #confirm}
     * — quando já {@code COMPLETED} (idempotência), {@code CANCELLED}/
     * {@code NO_SHOW} (BR-1: só sai de aberto), ou {@code agora} ainda não
     * alcançou {@code startsAt} (BR-3: não dá para concluir o que ainda não
     * começou).
     */
    public Appointment complete(Instant agora) {
        if (status == AppointmentStatus.COMPLETED
                || status == AppointmentStatus.CANCELLED
                || status == AppointmentStatus.NO_SHOW
                || agora.isBefore(startsAt)) {
            return this;
        }
        return new Appointment(
                id, tenantId, professionalId, serviceOfferingId, customerId,
                AppointmentStatus.COMPLETED, startsAt, endsAt, serviceName, durationMinutes, price, paymentStatus);
    }

    /**
     * Marca como pago (gestao-de-clientes, IDEA-006). Sem guard de transição
     * (BR-2): qualquer valor pode virar {@code PAID} a qualquer momento —
     * absorve (retorna {@code this}) só quando já está nesse valor.
     */
    public Appointment markPaymentAsPaid() {
        if (paymentStatus == PaymentStatus.PAID) {
            return this;
        }
        return new Appointment(
                id, tenantId, professionalId, serviceOfferingId, customerId,
                status, startsAt, endsAt, serviceName, durationMinutes, price, PaymentStatus.PAID);
    }

    /** Marca como pendente (gestao-de-clientes, IDEA-006). Mesma disciplina de {@link #markPaymentAsPaid()}. */
    public Appointment markPaymentAsPending() {
        if (paymentStatus == PaymentStatus.PENDING) {
            return this;
        }
        return new Appointment(
                id, tenantId, professionalId, serviceOfferingId, customerId,
                status, startsAt, endsAt, serviceName, durationMinutes, price, PaymentStatus.PENDING);
    }

    /**
     * Marca como fiado (gestao-de-clientes, IDEA-006) — o dono decide
     * conscientemente confiar no cliente e receber depois. Mesma disciplina
     * de {@link #markPaymentAsPaid()}.
     */
    public Appointment markPaymentAsOnCredit() {
        if (paymentStatus == PaymentStatus.ON_CREDIT) {
            return this;
        }
        return new Appointment(
                id, tenantId, professionalId, serviceOfferingId, customerId,
                status, startsAt, endsAt, serviceName, durationMinutes, price, PaymentStatus.ON_CREDIT);
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        return outro instanceof Appointment appointment && Objects.equals(id, appointment.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Sem dado de cliente (nome/telefone vivem em customer, nem aqui de qualquer forma). */
    @Override
    public String toString() {
        return "Appointment[id=%s, tenantId=%s, status=%s, startsAt=%s]"
                .formatted(id, tenantId, status, startsAt);
    }
}
