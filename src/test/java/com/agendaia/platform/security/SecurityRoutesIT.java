package com.agendaia.platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agendaia.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
// Boot 4 moveu esta anotação de org.springframework.boot.test.autoconfigure.web.servlet
// para cá, num jar novo (spring-boot-webmvc-test). Mesma modularização das
// auto-configurações que já tinha mordido no Flyway.
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifica a cadeia de filtros contra a aplicação montada.
 *
 * <p>Não há telas ainda: o que se verifica aqui é quem exige sessão, quem não
 * exige, e se o destino pretendido é preservado no redirecionamento.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityRoutesIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("rota do painel sem sessão redireciona para o login")
    void painelExigeSessao() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));
    }

    @Test
    @DisplayName("o destino pretendido é guardado para depois do login")
    void guardaODestinoPretendido() throws Exception {
        var resultado = mockMvc.perform(get("/admin/agenda"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        var savedRequest = resultado.getRequest().getSession(false);
        assertThat(savedRequest)
                .as("a sessão deve existir para guardar a requisição salva")
                .isNotNull();
    }

    @Test
    @DisplayName("cadastro é público e renderiza — é a porta de entrada do produto")
    void cadastroEPublico() throws Exception {
        mockMvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view()
                        .name("auth/cadastro"));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(roles = "OWNER")
    @DisplayName("página inexistente devolve 404, não 500")
    void paginaInexistenteDevolve404() throws Exception {
        // Precisa estar autenticado com ROLE_OWNER: rota desconhecida cai em
        // anyRequest().hasRole("OWNER") (BR-6/E2E-5, back-office-operador), e
        // quem não entrou é mandado ao login ANTES de chegar ao 404.
        //
        // Regressão do GlobalExceptionHandler: o tratamento genérico engolia as
        // exceções do Spring que já carregam status, e todo 404 virava 500.
        mockMvc.perform(get("/rota-que-nao-existe")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("health é público para a sonda do contêiner")
    void healthEPublico() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("demais endpoints do actuator não são públicos")
    void actuatorProtegido() throws Exception {
        // anyRequest().authenticated() + formLogin redireciona para o login.
        mockMvc.perform(get("/actuator/beans")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("o codificador de senha é BCrypt, e o mesmo texto gera hashes diferentes")
    void senhaUsaBcryptComSal() {
        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);

        var primeiro = passwordEncoder.encode("senha-do-joao");
        var segundo = passwordEncoder.encode("senha-do-joao");

        assertThat(primeiro).isNotEqualTo(segundo).startsWith("$2");
        assertThat(passwordEncoder.matches("senha-do-joao", primeiro)).isTrue();
        assertThat(passwordEncoder.matches("outra-senha", primeiro)).isFalse();
    }
}
