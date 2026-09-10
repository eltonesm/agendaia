package com.simboraagendar.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * Regressão determinística de BR-3 (pagina-institucional): a troca de cor de
 * marca é total e simultânea — nenhuma tela pode ficar com o indigo antigo
 * enquanto o resto do sistema já usa coral. Como o projeto não tem
 * infraestrutura de teste visual (LTP/E2E desabilitado), ler o arquivo de
 * origem é o jeito confiável de garantir isso, sem inspeção manual tela por
 * tela.
 */
class PaletaDeMarcaTest {

    @Test
    void corAntigaNaoSobrouEmNenhumToken() throws IOException {
        String conteudo = lerLayout();

        assertThat(conteudo.toLowerCase())
                .as("indigo antigo (#4f46e5/#4338ca) não pode sobrar em nenhum token de cor")
                .doesNotContain("4f46e5")
                .doesNotContain("4338ca");
    }

    @Test
    void corNovaEstaNaVariavelDeMarca() throws IOException {
        String conteudo = lerLayout();

        assertThat(conteudo.toLowerCase())
                .as("--bs-primary precisa refletir a cor de marca nova")
                .contains("--bs-primary: #ff6b4a");
    }

    private String lerLayout() throws IOException {
        try (InputStream in = new ClassPathResource("templates/fragments/layout.html").getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }
    }
}
