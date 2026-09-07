package com.agendaia.scheduling.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lista os agendamentos de um profissional num dia, para a agenda do dono
 * (US-1, agenda-profissional, TODO-008). {@code professionalId} sempre
 * revalidado contra o tenant da sessão — um id de outro tenant devolve
 * lista vazia, nunca lança (mesmo raciocínio de {@code find} nos demais
 * directories).
 */
public interface ViewAgendaUseCase {

    List<AgendaEntry> handle(UUID professionalId, LocalDate date);
}
