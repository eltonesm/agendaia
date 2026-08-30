package com.agendaia.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @RepeatedTest(50)
    @DisplayName("declara versão 7 e variante RFC 4122")
    void respeitaOFormato() {
        var uuid = UuidV7.generate();

        assertThat(uuid.version()).as("versão").isEqualTo(7);
        assertThat(uuid.variant()).as("variante").isEqualTo(2);
    }

    @Test
    @DisplayName("carrega o instante nos 48 bits mais significativos")
    void carregaOInstante() {
        var instante = 1_756_500_000_000L;

        var uuid = UuidV7.generate(instante);

        var extraido = uuid.getMostSignificantBits() >>> 16;
        assertThat(extraido).isEqualTo(instante);
    }

    @Test
    @DisplayName("ordena por tempo — é o motivo de existir, em vez de v4")
    void ordenaPorTempo() {
        var base = 1_756_500_000_000L;

        var emOrdemDeGeracao = List.of(
                UuidV7.generate(base),
                UuidV7.generate(base + 1),
                UuidV7.generate(base + 1000),
                UuidV7.generate(base + 86_400_000));

        var ordenados = new ArrayList<>(emOrdemDeGeracao);
        ordenados.sort((a, b) -> Long.compareUnsigned(
                a.getMostSignificantBits(), b.getMostSignificantBits()));

        assertThat(ordenados)
                .as("ordem lexicográfica deve bater com a ordem cronológica")
                .isEqualTo(emOrdemDeGeracao);
    }

    @Test
    @DisplayName("não repete dentro do mesmo milissegundo")
    void naoRepeteNoMesmoMilissegundo() {
        var instante = 1_756_500_000_000L;
        var gerados = new HashSet<>();

        for (var i = 0; i < 10_000; i++) {
            gerados.add(UuidV7.generate(instante));
        }

        assertThat(gerados).hasSize(10_000);
    }
}
