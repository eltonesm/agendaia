package com.agendaia.scheduling.domain;

import com.agendaia.shared.TimeRange;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * O pipeline de cálculo de disponibilidade (glossário): horário da empresa ∩
 * jornada do profissional − bloqueios → janelas livres → gerar starts
 * candidatos na grade fixa → filtrar quem comporta duração + intervalo
 * (BR-2, BR-3, BR-4).
 *
 * <p>Java puro — sem Spring, sem JPA (ADR 0002, regime completo de
 * {@code scheduling}). Método com nome próprio para gerar candidatos, não uma
 * interface com uma implementação — só existe {@code FIXED_GRID} no MVP
 * (ADR 0006).
 */
public final class SlotCalculator {

    /** Grade fixa de 10 minutos (ADR 0006) — constante do sistema, não configurável por tenant. */
    private static final int GRID_MINUTES = 10;

    private SlotCalculator() {
        // utilitário
    }

    /**
     * Calcula os horários candidatos válidos para uma combinação de
     * profissional, oferta e data.
     *
     * @param businessHours faixas de funcionamento do estabelecimento no dia da semana consultado
     * @param workSchedule faixas de jornada do profissional no mesmo dia da semana
     * @param blocked bloqueios (do profissional ou do estabelecimento inteiro) que se sobrepõem à data
     * @param durationMinutes duração da oferta
     * @param bufferMinutes intervalo reservado depois do atendimento
     * @return faixas {@code [início, início + duração)} dos candidatos válidos, na grade de 10 minutos
     */
    public static List<TimeRange> calculate(
            List<TimeRange> businessHours,
            List<TimeRange> workSchedule,
            List<TimeRange> blocked,
            int durationMinutes,
            int bufferMinutes) {
        var janelasLivres = intersectAll(businessHours, workSchedule);
        janelasLivres = subtractAll(janelasLivres, blocked);
        return gerarCandidatos(janelasLivres, durationMinutes, bufferMinutes);
    }

    /** BR-2: interseção par a par — cada faixa de empresa com cada faixa de jornada que a sobrepõe. */
    private static List<TimeRange> intersectAll(List<TimeRange> businessHours, List<TimeRange> workSchedule) {
        var resultado = new ArrayList<TimeRange>();
        for (var faixaEmpresa : businessHours) {
            for (var faixaJornada : workSchedule) {
                faixaEmpresa.intersect(faixaJornada).ifPresent(resultado::add);
            }
        }
        return resultado;
    }

    /** Remove cada bloqueio de cada janela livre, acumulando os pedaços restantes. */
    private static List<TimeRange> subtractAll(List<TimeRange> janelas, List<TimeRange> blocked) {
        var restantes = janelas;
        for (var bloqueio : blocked) {
            var depoisDoBloqueio = new ArrayList<TimeRange>();
            for (var janela : restantes) {
                depoisDoBloqueio.addAll(janela.subtract(bloqueio));
            }
            restantes = depoisDoBloqueio;
        }
        return restantes;
    }

    /** BR-3/BR-4: candidatos a cada 10 min, mantidos só se duração + intervalo couber inteiro na janela. */
    private static List<TimeRange> gerarCandidatos(List<TimeRange> janelasLivres, int durationMinutes, int bufferMinutes) {
        var passo = Duration.ofMinutes(GRID_MINUTES);
        var ocupado = Duration.ofMinutes((long) durationMinutes + bufferMinutes);
        var visivel = Duration.ofMinutes(durationMinutes);

        var candidatos = new ArrayList<TimeRange>();
        for (var janela : janelasLivres) {
            var inicio = janela.start();
            while (!inicio.plus(ocupado).isAfter(janela.end())) {
                candidatos.add(new TimeRange(inicio, inicio.plus(visivel)));
                inicio = inicio.plus(passo);
            }
        }
        return candidatos;
    }
}
