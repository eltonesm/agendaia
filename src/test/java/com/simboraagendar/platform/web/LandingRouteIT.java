package com.simboraagendar.platform.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.simboraagendar.TestcontainersConfiguration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * "/" passou a renderizar a landing institucional em vez de redirecionar
 * para /cadastro (pagina-institucional, E2E-1/E2E-2, DD-1).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LandingRouteIT {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("raiz renderiza a landing, não redireciona mais para /cadastro")
    void raizRenderizaLanding() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landing"));
    }

    @Test
    @DisplayName("botão de criar conta aponta para /cadastro")
    void botaoCriarContaApontaParaCadastro() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("href=\"/cadastro\"")));
    }

    @Test
    @DisplayName("/cadastro continua público e sem mudança de comportamento")
    void cadastroContinuaFuncionando() throws Exception {
        mockMvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/cadastro"));
    }
}
