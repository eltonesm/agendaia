package com.simboraagendar.scheduling.domain;

/**
 * Status de pagamento de um {@link Appointment} — conceito independente de
 * {@link AppointmentStatus} (gestao-de-clientes, IDEA-006): responde "foi
 * pago?", nunca "o atendimento aconteceu?". Pode mudar em qualquer direção,
 * a qualquer momento — não é uma máquina de estados com transição proibida.
 *
 * <p>{@code ON_CREDIT} é o conceito de "fiado": o dono decide conscientemente
 * confiar no cliente e receber depois, diferente de {@code PENDING}, que é
 * só "ainda não pagou, mas vai já".
 */
public enum PaymentStatus {
    PAID,
    PENDING,
    ON_CREDIT
}
