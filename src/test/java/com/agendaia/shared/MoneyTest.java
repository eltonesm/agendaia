package com.agendaia.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring, sem banco — puro cálculo. */
class MoneyTest {

    @Test
    @DisplayName("converte reais em centavos sem erro de arredondamento")
    void converteReaisEmCentavos() {
        assertThat(Money.reais(new BigDecimal("30.00")).cents()).isEqualTo(3000);
        assertThat(Money.reais(new BigDecimal("0.05")).cents()).isEqualTo(5);
        assertThat(Money.reais(new BigDecimal("199.99")).cents()).isEqualTo(19999);
    }

    @Test
    @DisplayName("arredonda para o centavo mais próximo quando o valor tem mais casas")
    void arredondaParaOCentavoMaisProximo() {
        assertThat(Money.reais(new BigDecimal("30.005")).cents()).isEqualTo(3001);
        assertThat(Money.reais(new BigDecimal("30.004")).cents()).isEqualTo(3000);
    }

    @Test
    @DisplayName("zero() e reais(0) representam o mesmo valor")
    void zeroERepresentavelExplicitamente() {
        assertThat(Money.zero().cents()).isZero();
        assertThat(Money.reais(BigDecimal.ZERO).cents()).isZero();
    }

    @Test
    @DisplayName("recusa valor nulo")
    void recusaValorNulo() {
        assertThatThrownBy(() -> Money.reais(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("recusa centavos negativos, direto no construtor")
    void recusaCentavosNegativos() {
        assertThatThrownBy(() -> new Money(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    @DisplayName("recusa valor negativo vindo de BigDecimal")
    void recusaValorNegativoDeBigDecimal() {
        assertThatThrownBy(() -> Money.reais(new BigDecimal("-10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("formata sempre com duas casas, mesmo quando o centavo termina em zero")
    void formataComDuasCasas() {
        assertThat(new Money(3000).format()).isEqualTo("R$ 30,00");
        assertThat(new Money(3005).format()).isEqualTo("R$ 30,05");
        assertThat(new Money(5).format()).isEqualTo("R$ 0,05");
        assertThat(Money.zero().format()).isEqualTo("R$ 0,00");
    }

    @Test
    @DisplayName("dois valores com o mesmo centavo sao iguais — record")
    void igualdadePorValor() {
        assertThat(Money.reais(new BigDecimal("30.00"))).isEqualTo(new Money(3000));
    }
}
