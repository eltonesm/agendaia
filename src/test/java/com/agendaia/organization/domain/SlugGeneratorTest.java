package com.agendaia.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Sem Spring, sem banco, milissegundos — é o que o regime de domínio puro compra.
 */
class SlugGeneratorTest {

    @Nested
    @DisplayName("derivação a partir do nome")
    class Derivacao {

        @ParameterizedTest(name = "\"{0}\" vira \"{1}\"")
        @CsvSource({
            "Barbearia do João,      barbearia-do-joao",
            "Salão & Cia.,           salao-cia",
            "'Studio  da   Ana',     studio-da-ana",
            "Corte 10,               corte-10",
            "--Barbearia--,          barbearia",
            "BARBEARIA,              barbearia",
            "Açaí & Cabelo,          acai-cabelo",
            "Beleza Pura,            beleza-pura"
        })
        @DisplayName("casos da spec funcional")
        void derivaConformeAEspecificacao(String nome, String esperado) {
            assertThat(SlugGenerator.from(nome)).isEqualTo(esperado);
        }

        @ParameterizedTest
        @ValueSource(strings = {"!!!", "---", "   ", "@#$%"})
        @DisplayName("nome sem caractere aproveitável devolve vazio, não um slug inventado")
        void devolveVazioQuandoNadaSobra(String nome) {
            assertThat(SlugGenerator.from(nome)).isEmpty();
        }

        @Test
        @DisplayName("null e branco devolvem vazio, sem estourar")
        void toleraAusenciaDeNome() {
            assertThat(SlugGenerator.from(null)).isEmpty();
            assertThat(SlugGenerator.from("")).isEmpty();
        }

        @Test
        @DisplayName("nome longo é truncado sem deixar hífen na ponta")
        void truncaSemDeixarHifenSolto() {
            var nomeLongo = "Barbearia do Joao ".repeat(10);

            var slug = SlugGenerator.from(nomeLongo);

            assertThat(slug).hasSizeLessThanOrEqualTo(SlugGenerator.MAX_LENGTH);
            assertThat(slug).doesNotEndWith("-").doesNotStartWith("-");
            assertThat(SlugGenerator.hasValidFormat(slug)).isTrue();
        }

        @Test
        @DisplayName("o que a derivação produz é sempre aceito pela validação")
        void oQueDerivaEValido() {
            var slug = SlugGenerator.from("Barbearia do João");

            assertThat(SlugGenerator.hasValidFormat(slug)).isTrue();
        }
    }

    @Nested
    @DisplayName("validação do formato submetido")
    class Validacao {

        @ParameterizedTest
        @ValueSource(strings = {"abc", "barbearia-do-joao", "corte-10", "a1b", "x2y-z9"})
        @DisplayName("aceita minúsculas, números e hífen no meio")
        void aceitaFormatoValido(String slug) {
            assertThat(SlugGenerator.hasValidFormat(slug)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "ab",              // curto demais
            "-barbearia",      // hífen no início
            "barbearia-",      // hífen no fim
            "Barbearia",       // maiúscula
            "barbearia joao",  // espaço
            "barbearia_joao",  // sublinhado
            "barbeariá",       // acento
            "barbearia/joao"   // barra
        })
        @DisplayName("recusa o que quebraria a rota ou a coluna")
        void recusaFormatoInvalido(String slug) {
            assertThat(SlugGenerator.hasValidFormat(slug)).isFalse();
        }

        @Test
        @DisplayName("recusa null e slug acima do limite da coluna")
        void recusaNullEExcessivamenteLongo() {
            assertThat(SlugGenerator.hasValidFormat(null)).isFalse();
            assertThat(SlugGenerator.hasValidFormat("a".repeat(SlugGenerator.MAX_LENGTH + 1)))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("variação sugerida quando o link está tomado")
    class Variacao {

        @Test
        @DisplayName("acrescenta o número ao fim")
        void acrescentaONumero() {
            assertThat(SlugGenerator.variation("barbearia-do-joao", 2))
                    .isEqualTo("barbearia-do-joao-2");
        }

        @Test
        @DisplayName("encurta a raiz em vez de estourar o limite da coluna")
        void respeitaOLimite() {
            var noLimite = "a".repeat(SlugGenerator.MAX_LENGTH);

            var variacao = SlugGenerator.variation(noLimite, 2);

            assertThat(variacao).hasSizeLessThanOrEqualTo(SlugGenerator.MAX_LENGTH);
            assertThat(SlugGenerator.hasValidFormat(variacao)).isTrue();
        }

        @Test
        @DisplayName("não deixa hífen duplo quando o corte cai em cima de um")
        void naoDeixaHifenDuplo() {
            // O corte para caber cairia logo depois de um hífen, e "...-" + "-2"
            // daria "--2", que o formato recusa.
            var comHifenNoCorte = "a".repeat(SlugGenerator.MAX_LENGTH - 3) + "-bc";

            var variacao = SlugGenerator.variation(comHifenNoCorte, 2);

            assertThat(variacao).doesNotContain("--");
            assertThat(SlugGenerator.hasValidFormat(variacao)).isTrue();
        }

        @Test
        @DisplayName("toda variação de 2 a 9 continua um slug válido")
        void todasAsVariacoesSaoValidas() {
            for (var n = 2; n <= 9; n++) {
                assertThat(SlugGenerator.hasValidFormat(
                                SlugGenerator.variation("barbearia-do-joao", n)))
                        .as("variação %d", n)
                        .isTrue();
            }
        }
    }
}
