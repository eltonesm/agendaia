package com.agendaia.organization.api;

import com.agendaia.shared.TimeRange;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Segundo ponto de entrada de outros contextos em {@code organization}
 * (consultar-horarios-disponiveis, TODO-005) — dado declarado de
 * disponibilidade (glossário), sempre já convertido para {@link TimeRange}.
 *
 * <p>Sem tenant nos argumentos: lido de {@code TenantContext.require()} por
 * dentro da implementação, mesma convenção de {@link ProfessionalDirectory}.
 */
public interface AvailabilityDirectory {

    /** Faixas de funcionamento do estabelecimento no dia da semana. */
    List<TimeRange> operatingHoursFor(DayOfWeek dayOfWeek);

    /** Faixas de jornada do profissional no dia da semana. */
    List<TimeRange> workScheduleFor(UUID professionalId, DayOfWeek dayOfWeek);

    /**
     * Bloqueios (do profissional específico, ou do estabelecimento inteiro)
     * que se sobrepõem à data, já recortados para o intervalo
     * {@code [00:00, 24:00)} local dessa data.
     */
    List<TimeRange> blocksFor(UUID professionalId, LocalDate date);
}
