package com.agendaia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fronteira <strong>entre</strong> contextos delimitados.
 *
 * <p>Escopo do Spring Modulith, conforme o ADR 0010: encapsulamento (o que é
 * interno de um contexto é inacessível de fora), ausência de ciclos, e
 * dependências declaradas em vez de inferidas.
 *
 * <p>As camadas <strong>dentro</strong> de cada contexto são responsabilidade do
 * {@link ArchitectureTest}. Os dois escopos não se sobrepõem.
 *
 * <p>O Modulith trata cada subpacote direto de {@code com.agendaia} como um
 * módulo. {@code shared} e {@code platform} são declarados abertos: seus
 * internos existem justamente para ser usados pelos contextos.
 */
class ModuleStructureTest {

    private static final ApplicationModules MODULOS =
            ApplicationModules.of(AgendaIaApplication.class);

    @Test
    @DisplayName("os seis contextos são reconhecidos como módulos")
    void osContextosSaoModulos() {
        // Modulith 2.x trocou getName() por getIdentifier().
        var nomes = MODULOS.stream().map(m -> m.getIdentifier().toString()).toList();

        assertThat(nomes)
                .containsExactlyInAnyOrder(
                        "shared", "platform", "organization", "catalog", "scheduling", "customer");
    }

    @Test
    @DisplayName("nenhum contexto viola encapsulamento nem forma ciclo")
    void estruturaDeModulosEValida() {
        MODULOS.verify();
    }
}
