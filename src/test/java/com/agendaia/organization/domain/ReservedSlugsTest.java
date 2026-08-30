package com.agendaia.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReservedSlugsTest {

    @ParameterizedTest
    @ValueSource(strings = {"admin", "login", "logout", "api", "actuator", "www", "cadastro"})
    @DisplayName("recusa as palavras que protegem rota do sistema")
    void recusaPalavraReservada(String slug) {
        assertThat(ReservedSlugs.contains(slug)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"b", "js"})
    @DisplayName("não guarda palavra curta demais para ser slug — já é recusada pelo formato")
    void naoGuardaPalavraCurta(String curta) {
        assertThat(SlugGenerator.hasValidFormat(curta))
                .as("'%s' já é recusada pelo formato", curta)
                .isFalse();
        assertThat(ReservedSlugs.contains(curta))
                .as("'%s' na lista de reservadas seria peso morto", curta)
                .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "Login", "ApI"})
    @DisplayName("a comparação ignora maiúsculas")
    void ignoraCaixa(String slug) {
        assertThat(ReservedSlugs.contains(slug)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"barbearia-do-joao", "salao-da-maria", "administracao", "loginhouse"})
    @DisplayName("aceita slug legítimo, inclusive o que apenas contém palavra reservada")
    void aceitaSlugLegitimo(String slug) {
        assertThat(ReservedSlugs.contains(slug)).isFalse();
    }

    @Test
    @DisplayName("null e branco não são reservados — quem trata ausência é o validador de formato")
    void toleraAusencia() {
        assertThat(ReservedSlugs.contains(null)).isFalse();
        assertThat(ReservedSlugs.contains("  ")).isFalse();
    }

    @Test
    @DisplayName("toda palavra reservada tem formato de slug válido")
    void reservadasSaoSlugsValidos() {
        assertThat(ReservedSlugs.all())
                .allSatisfy(slug -> assertThat(SlugGenerator.hasValidFormat(slug))
                        .as("'%s' não teria formato válido, logo nunca seria escolhido "
                                + "e não precisaria estar na lista", slug)
                        .isTrue());
    }
}
