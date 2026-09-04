package com.agendaia.scheduling.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * Data consultada fora de [hoje, hoje + 30 dias] (BR-5).
 *
 * <p>Erro deliberado, não lista vazia — é a chamada errada (consumidor não
 * deveria ter oferecido essa data), diferente de "sem expediente naquele
 * dia", que é uma resposta válida vazia (BR-6).
 */
public class AvailabilityQueryOutOfRangeException extends DomainException {

    public AvailabilityQueryOutOfRangeException() {
        super("Data fora do intervalo permitido para consulta de disponibilidade (até 30 dias a partir de hoje).");
    }
}
