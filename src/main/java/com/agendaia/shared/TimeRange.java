package com.agendaia.shared;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Um intervalo de horário, meio-aberto {@code [start, end)} — dois intervalos
 * encostados (fim de um igual ao início do outro) não se sobrepõem.
 *
 * <p>Mesma semântica que {@code WorkSchedule.overlaps()} (TODO-004) já usava
 * inline, antes deste tipo existir. Nasce reutilizável em
 * {@code consultar-horarios-disponiveis} (TODO-005), que precisa de
 * interseção e subtração de intervalos, não só sobreposição.
 */
public record TimeRange(LocalTime start, LocalTime end) {

    public TimeRange {
        if (start == null || end == null) {
            throw new IllegalArgumentException("intervalo precisa de início e fim");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("fim deve ser depois do início");
        }
    }

    /** Sobreposição no tempo, sentido meio-aberto {@code [)}. */
    public boolean overlaps(TimeRange outro) {
        return this.start.isBefore(outro.end) && outro.start.isBefore(this.end);
    }

    /** Vazio se não há sobreposição. */
    public Optional<TimeRange> intersect(TimeRange outro) {
        if (!overlaps(outro)) {
            return Optional.empty();
        }
        var inicio = this.start.isAfter(outro.start) ? this.start : outro.start;
        var fim = this.end.isBefore(outro.end) ? this.end : outro.end;
        return Optional.of(new TimeRange(inicio, fim));
    }

    /**
     * Remove {@code bloqueio} deste intervalo. Sem sobreposição, devolve este
     * intervalo inteiro; sobreposição total, lista vazia; sobreposição
     * parcial, um ou dois pedaços restantes — é assim que um bloqueio no meio
     * do dia parte uma janela livre em duas.
     */
    public List<TimeRange> subtract(TimeRange bloqueio) {
        if (!overlaps(bloqueio)) {
            return List.of(this);
        }
        var pedacos = new ArrayList<TimeRange>(2);
        if (bloqueio.start.isAfter(this.start)) {
            pedacos.add(new TimeRange(this.start, bloqueio.start));
        }
        if (bloqueio.end.isBefore(this.end)) {
            pedacos.add(new TimeRange(bloqueio.end, this.end));
        }
        return pedacos;
    }

    public Duration duration() {
        return Duration.between(start, end);
    }
}
