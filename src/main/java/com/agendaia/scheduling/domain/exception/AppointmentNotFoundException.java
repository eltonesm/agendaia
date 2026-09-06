package com.agendaia.scheduling.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * O agendamento informado não existe, ou existe em outro tenant (BR-1/BR-7
 * de confirmacao-e-cancelamento, TODO-007).
 *
 * <p>Mesma mensagem para os dois casos, de propósito — mesmo raciocínio de
 * {@code ServiceOfferingNotFoundException} (pagina-publica-agendamento).
 * A checagem é {@code AppointmentRepository.findByTenantIdAndId} devolvendo
 * vazio; quem decide lançar esta exceção é o handler, não o repositório.
 */
public class AppointmentNotFoundException extends DomainException {

    public AppointmentNotFoundException() {
        super("Agendamento não encontrado.");
    }
}
