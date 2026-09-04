package com.agendaia.scheduling.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.agendaia.shared.TimeRange;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring, sem banco — puro cálculo (ADR 0002). */
class SlotCalculatorTest {

    private static TimeRange faixa(String inicio, String fim) {
        return new TimeRange(LocalTime.parse(inicio), LocalTime.parse(fim));
    }

    @Test
    @DisplayName("caminho feliz: grade de 10 min dentro de uma janela livre única")
    void caminhoFeliz() {
        var businessHours = List.of(faixa("08:00", "12:00"));
        var workSchedule = List.of(faixa("08:00", "12:00"));

        var candidatos = SlotCalculator.calculate(businessHours, workSchedule, List.of(), 30, 0);

        // 08:00, 08:10, ..., até o último que cabe 30 min antes de 12:00 -> 11:30
        assertThat(candidatos).hasSize(22);
        assertThat(candidatos.getFirst()).isEqualTo(faixa("08:00", "08:30"));
        assertThat(candidatos.getLast()).isEqualTo(faixa("11:30", "12:00"));
    }

    @Test
    @DisplayName("almoço: duas janelas de jornada no mesmo dia, nenhum candidato no vão")
    void almocoEntreDuasFaixas() {
        var businessHours = List.of(faixa("08:00", "18:00"));
        var workSchedule = List.of(faixa("08:00", "12:00"), faixa("13:00", "18:00"));

        var candidatos = SlotCalculator.calculate(businessHours, workSchedule, List.of(), 30, 0);

        assertThat(candidatos).noneMatch(c -> c.start().isAfter(LocalTime.parse("11:30"))
                && c.start().isBefore(LocalTime.parse("13:00")));
        assertThat(candidatos.getFirst()).isEqualTo(faixa("08:00", "08:30"));
        assertThat(candidatos.getLast()).isEqualTo(faixa("17:30", "18:00"));
    }

    @Test
    @DisplayName("bloqueio no meio do dia remove só a janela bloqueada")
    void bloqueioNoMeioDoDia() {
        var businessHours = List.of(faixa("08:00", "18:00"));
        var workSchedule = List.of(faixa("08:00", "18:00"));
        var blocked = List.of(faixa("10:00", "11:00"));

        var candidatos = SlotCalculator.calculate(businessHours, workSchedule, blocked, 30, 0);

        // Janela [08:00,10:00) rende 10 candidatos, janela [11:00,18:00) rende 40 -- nenhum entre 10:00 e 11:00.
        assertThat(candidatos).hasSize(50);
        assertThat(candidatos).contains(faixa("09:30", "10:00"), faixa("11:00", "11:30"));
        assertThat(candidatos)
                .noneMatch(c -> !c.start().isBefore(LocalTime.parse("10:00")) && c.start().isBefore(LocalTime.parse("11:00")));
    }

    @Test
    @DisplayName("empresa fechada no dia: nenhuma faixa de expediente, lista vazia")
    void empresaFechada() {
        var candidatos = SlotCalculator.calculate(List.of(), List.of(faixa("08:00", "12:00")), List.of(), 30, 0);

        assertThat(candidatos).isEmpty();
    }

    @Test
    @DisplayName("profissional sem jornada no dia: lista vazia mesmo com empresa aberta")
    void profissionalSemJornada() {
        var candidatos = SlotCalculator.calculate(List.of(faixa("08:00", "18:00")), List.of(), List.of(), 30, 0);

        assertThat(candidatos).isEmpty();
    }

    @Test
    @DisplayName("jornada fora do expediente da empresa: interseção vazia, sem candidato")
    void jornadaForaDoExpediente() {
        var businessHours = List.of(faixa("08:00", "12:00"));
        var workSchedule = List.of(faixa("14:00", "18:00"));

        var candidatos = SlotCalculator.calculate(businessHours, workSchedule, List.of(), 30, 0);

        assertThat(candidatos).isEmpty();
    }

    @Test
    @DisplayName("oferta maior que qualquer janela livre: nenhum candidato cabe")
    void ofertaMaiorQueAJanela() {
        var businessHours = List.of(faixa("08:00", "09:00"));
        var workSchedule = List.of(faixa("08:00", "09:00"));

        var candidatos = SlotCalculator.calculate(businessHours, workSchedule, List.of(), 90, 0);

        assertThat(candidatos).isEmpty();
    }

    @Test
    @DisplayName("bufferMinutes reduz o último candidato possível, mas não aparece no horário mostrado")
    void bufferReduzUltimoCandidato() {
        // Janela de 08:00 a 09:00, oferta de 30 min + 10 min de buffer = 40 min ocupados.
        // Sem buffer, o último candidato seria 08:30-09:00. Com buffer, precisa caber 40 min: só até 08:20.
        var janela = List.of(faixa("08:00", "09:00"));

        var comBuffer = SlotCalculator.calculate(janela, janela, List.of(), 30, 10);
        var semBuffer = SlotCalculator.calculate(janela, janela, List.of(), 30, 0);

        assertThat(comBuffer.getLast()).isEqualTo(faixa("08:20", "08:50"));
        assertThat(semBuffer.getLast()).isEqualTo(faixa("08:30", "09:00"));
        // O horário mostrado nunca inclui o buffer, mesmo quando ele restringe o candidato.
        assertThat(comBuffer.getLast().duration().toMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("dois profissionais/janelas distintas via multiplas faixas de business hours")
    void multiplasFaixasDeExpediente() {
        var businessHours = List.of(faixa("08:00", "12:00"), faixa("14:00", "16:00"));
        var workSchedule = List.of(faixa("08:00", "12:00"), faixa("14:00", "16:00"));

        var candidatos = SlotCalculator.calculate(businessHours, workSchedule, List.of(), 20, 0);

        assertThat(candidatos).contains(faixa("08:00", "08:20"));
        assertThat(candidatos).contains(faixa("14:00", "14:20"));
        assertThat(candidatos.getLast()).isEqualTo(faixa("15:40", "16:00"));
    }
}
