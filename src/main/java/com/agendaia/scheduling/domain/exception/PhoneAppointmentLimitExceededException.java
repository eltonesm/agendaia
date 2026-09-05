package com.agendaia.scheduling.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * O telefone já tem o máximo de agendamentos futuros ativos permitido no
 * mesmo estabelecimento (US-7, BR-9). Ao contrário do rate limit por IP, a
 * causa pode ser explícita — não ajuda bot nenhum saber "você já tem 3
 * agendamentos".
 */
public class PhoneAppointmentLimitExceededException extends DomainException {

    public PhoneAppointmentLimitExceededException() {
        super(
                "Esse telefone já tem o número máximo de agendamentos futuros permitido. "
                        + "Aguarde um deles acontecer, ou cancele algum antes de marcar outro.",
                "phone");
    }
}
