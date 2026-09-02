package com.agendaia.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Um valor em reais, guardado como centavos.
 *
 * <p>Inteiro, nunca {@code double}/{@code float}: dinheiro em ponto flutuante
 * é erro conhecido — {@code 0.1 + 0.2 != 0.3}. A conversão de/para
 * {@link BigDecimal} acontece uma vez só, na fábrica; depois disso é
 * aritmética inteira, sem arredondamento possível.
 *
 * <p><strong>Sem soma, subtração ou comparação nesta versão.</strong> Nenhum
 * critério de aceite do cadastro de oferta precisa disso — guardar e exibir é
 * tudo que a feature pede. Acrescentar depois é extensão aditiva, não
 * migração.
 */
public record Money(long cents) {

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("valor não pode ser negativo");
        }
    }

    public static Money zero() {
        return new Money(0);
    }

    /** Converte o que o formulário envia (ex.: {@code 30.00}) para centavos. */
    public static Money reais(BigDecimal reais) {
        if (reais == null) {
            throw new IllegalArgumentException("valor não pode ser nulo");
        }
        return new Money(reais.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact());
    }

    /**
     * Formato fixo "R$ X,YY", sem separador de milhar.
     *
     * <p>Deliberadamente sem {@link java.text.NumberFormat}: um preço de
     * serviço de barbearia nunca chega perto da casa do milhar, e formatar à
     * mão evita depender dos dados de moeda do fuso/locale do ambiente onde a
     * aplicação roda.
     */
    public String format() {
        long reaisPart = cents / 100;
        long centavosPart = Math.abs(cents % 100);
        return "R$ %d,%02d".formatted(reaisPart, centavosPart);
    }
}
