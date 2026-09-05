package com.agendaia.scheduling.domain;

/**
 * Status de um {@link Appointment} (glossário). Só {@link #SCHEDULED} é
 * alcançável em pagina-publica-agendamento (TODO-006) — os demais entram
 * com as features que os usam (TODO-007/008).
 */
public enum AppointmentStatus {

    /** Marcado, aguardando o atendimento. Único status que esta feature grava. */
    SCHEDULED,

    /** Confirmado pelo cliente (link da TODO-007). */
    CONFIRMED,

    /** Cancelado pelo cliente ou pelo profissional. Libera o horário. */
    CANCELLED,

    /** Cliente não compareceu. Libera o horário retroativamente. */
    NO_SHOW
}
