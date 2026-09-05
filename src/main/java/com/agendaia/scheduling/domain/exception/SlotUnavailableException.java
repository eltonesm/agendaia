package com.agendaia.scheduling.domain.exception;

import com.agendaia.shared.DomainException;

/**
 * O horário escolhido acabou de ser reservado por outra requisição (US-6,
 * BR-4/ADR 0005). Traduzida pelo adapter de persistência a partir da
 * violação da exclusion constraint — nunca lançada pelo domínio, que não
 * tem como saber disso sozinho.
 */
public class SlotUnavailableException extends DomainException {

    public SlotUnavailableException() {
        super("Esse horário acabou de ser reservado. Escolha outro horário disponível.");
    }
}
