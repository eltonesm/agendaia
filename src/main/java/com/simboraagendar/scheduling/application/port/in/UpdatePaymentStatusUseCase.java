package com.simboraagendar.scheduling.application.port.in;

import com.simboraagendar.scheduling.domain.PaymentStatus;
import java.util.UUID;

/** Marca o status de pagamento de um agendamento (gestao-de-clientes, US-4). */
public interface UpdatePaymentStatusUseCase {

    void updatePaymentStatus(UUID appointmentId, PaymentStatus status);
}
