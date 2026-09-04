package com.agendaia.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring, sem banco — puro cálculo. */
class TimeRangeTest {

    private static TimeRange faixa(String inicio, String fim) {
        return new TimeRange(LocalTime.parse(inicio), LocalTime.parse(fim));
    }

    @Test
    @DisplayName("recusa fim igual ou anterior ao início")
    void recusaFimInvalido() {
        assertThatThrownBy(() -> faixa("10:00", "10:00")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> faixa("10:00", "09:00")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("faixas encostadas não se sobrepõem — mecanismo do intervalo de almoço")
    void faixasEncostadasNaoSeSobrepoem() {
        var manha = faixa("08:00", "12:00");
        var tarde = faixa("12:00", "18:00");

        assertThat(manha.overlaps(tarde)).isFalse();
        assertThat(tarde.overlaps(manha)).isFalse();
        assertThat(manha.intersect(tarde)).isEmpty();
    }

    @Test
    @DisplayName("faixas parcialmente sobrepostas se sobrepõem e interseção é o trecho comum")
    void faixasParcialmenteSobrepostas() {
        var a = faixa("08:00", "12:00");
        var b = faixa("10:00", "14:00");

        assertThat(a.overlaps(b)).isTrue();
        assertThat(a.intersect(b)).contains(faixa("10:00", "12:00"));
        assertThat(b.intersect(a)).contains(faixa("10:00", "12:00"));
    }

    @Test
    @DisplayName("uma faixa contida na outra: interseção é a menor")
    void faixaContida() {
        var externa = faixa("08:00", "18:00");
        var interna = faixa("10:00", "12:00");

        assertThat(externa.intersect(interna)).contains(interna);
    }

    @Test
    @DisplayName("subtract sem sobreposição devolve a faixa inteira")
    void subtractSemSobreposicao() {
        var janela = faixa("08:00", "12:00");
        var bloqueio = faixa("13:00", "14:00");

        assertThat(janela.subtract(bloqueio)).containsExactly(janela);
    }

    @Test
    @DisplayName("subtract de bloqueio que cobre a faixa inteira devolve lista vazia")
    void subtractBloqueioCobreTudo() {
        var janela = faixa("08:00", "12:00");
        var bloqueio = faixa("07:00", "13:00");

        assertThat(janela.subtract(bloqueio)).isEmpty();
    }

    @Test
    @DisplayName("subtract de bloqueio exatamente igual à faixa devolve lista vazia")
    void subtractBloqueioIgualAFaixa() {
        var janela = faixa("08:00", "12:00");

        assertThat(janela.subtract(janela)).isEmpty();
    }

    @Test
    @DisplayName("subtract de bloqueio no meio parte a faixa em dois pedaços")
    void subtractBloqueioNoMeio() {
        var janela = faixa("08:00", "18:00");
        var almoco = faixa("12:00", "13:00");

        assertThat(janela.subtract(almoco)).containsExactly(faixa("08:00", "12:00"), faixa("13:00", "18:00"));
    }

    @Test
    @DisplayName("subtract de bloqueio no início deixa só o pedaço final")
    void subtractBloqueioNoInicio() {
        var janela = faixa("08:00", "12:00");
        var bloqueio = faixa("08:00", "10:00");

        assertThat(janela.subtract(bloqueio)).containsExactly(faixa("10:00", "12:00"));
    }

    @Test
    @DisplayName("subtract de bloqueio no fim deixa só o pedaço inicial")
    void subtractBloqueioNoFim() {
        var janela = faixa("08:00", "12:00");
        var bloqueio = faixa("10:00", "12:00");

        assertThat(janela.subtract(bloqueio)).containsExactly(faixa("08:00", "10:00"));
    }

    @Test
    @DisplayName("duration() calcula o tamanho do intervalo")
    void duracao() {
        assertThat(faixa("08:00", "08:30").duration().toMinutes()).isEqualTo(30);
    }
}
