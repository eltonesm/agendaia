package com.agendaia.organization.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * Duas faixas do mesmo profissional, no mesmo dia, se sobrepõem no tempo
 * (BR-3).
 *
 * <p>Faixas encostadas (fim de uma igual ao início da outra) não disparam
 * esta exceção — é o mecanismo do intervalo de almoço.
 */
public class WorkScheduleOverlapException extends DomainException {

    public WorkScheduleOverlapException() {
        super("Esta faixa se sobrepõe a outra já cadastrada para este profissional neste dia.", "startsAt");
    }
}
