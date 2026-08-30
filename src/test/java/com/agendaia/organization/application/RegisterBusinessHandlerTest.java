package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.organization.application.command.RegisterBusinessCommand;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.organization.domain.User;
import com.agendaia.organization.domain.UserRepository;
import com.agendaia.organization.domain.exception.EmailAlreadyUsedException;
import com.agendaia.organization.domain.exception.SlugUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class RegisterBusinessHandlerTest {

    @Mock private BusinessRepository businessRepository;
    @Mock private UserRepository userRepository;

    private RegisterBusinessHandler handler;

    @BeforeEach
    void montar() {
        handler = new RegisterBusinessHandler(
                businessRepository, userRepository, new BCryptPasswordEncoder());
    }

    private RegisterBusinessCommand comando() {
        return new RegisterBusinessCommand(
                "Barbearia do João", "barbearia-do-joao", "joao@exemplo.com", "senha-do-joao");
    }

    @Test
    @DisplayName("cria estabelecimento e dono, e devolve o que a camada web precisa")
    void criaOsDois() {
        when(businessRepository.existsBySlug("barbearia-do-joao")).thenReturn(false);
        when(userRepository.existsByEmail("joao@exemplo.com")).thenReturn(false);

        var resultado = handler.register(comando());

        assertThat(resultado.businessName()).isEqualTo("Barbearia do João");
        assertThat(resultado.slug()).isEqualTo("barbearia-do-joao");
        assertThat(resultado.ownerEmail()).isEqualTo("joao@exemplo.com");
        assertThat(resultado.tenantId().value()).isEqualTo(resultado.businessId());

        verify(businessRepository).saveAndFlush(any(Business.class));
        verify(userRepository).saveAndFlush(any(User.class));
    }

    @Test
    @DisplayName("a senha é gravada como hash BCrypt, nunca em texto claro")
    void senhaViraHash() {
        when(businessRepository.existsBySlug(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);

        handler.register(comando());

        var capturado = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(capturado.capture());
        assertThat(capturado.getValue().passwordHash())
                .startsWith("$2")
                .isNotEqualTo("senha-do-joao");
    }

    @Test
    @DisplayName("slug em uso recusa antes de gravar qualquer coisa")
    void slugEmUso() {
        when(businessRepository.existsBySlug("barbearia-do-joao")).thenReturn(true);

        assertThatThrownBy(() -> handler.register(comando()))
                .isInstanceOf(SlugUnavailableException.class)
                .hasFieldOrPropertyWithValue("field", "slug");

        verify(businessRepository, never()).saveAndFlush(any());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "login", "actuator", "cadastro"})
    @DisplayName("palavra reservada é recusada sem nem consultar o banco")
    void slugReservado(String reservado) {
        var comando = new RegisterBusinessCommand(
                "Barbearia", reservado, "joao@exemplo.com", "senha-do-joao");

        assertThatThrownBy(() -> handler.register(comando))
                .isInstanceOf(SlugUnavailableException.class);

        verify(businessRepository, never()).existsBySlug(any());
    }

    @Test
    @DisplayName("e-mail já cadastrado recusa antes de gravar")
    void emailEmUso() {
        when(businessRepository.existsBySlug(any())).thenReturn(false);
        when(userRepository.existsByEmail("joao@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> handler.register(comando()))
                .isInstanceOf(EmailAlreadyUsedException.class)
                .hasFieldOrPropertyWithValue("field", "email");

        verify(businessRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("corrida no slug: o banco recusa e vira erro de campo, não erro interno")
    void corridaNoSlug() {
        when(businessRepository.existsBySlug(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(businessRepository.saveAndFlush(any(Business.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "erro", new RuntimeException(
                                "duplicate key value violates unique constraint "
                                        + "\"business_slug_unique\"")));

        assertThatThrownBy(() -> handler.register(comando()))
                .isInstanceOf(SlugUnavailableException.class);
    }

    @Test
    @DisplayName("corrida no e-mail é traduzida para o campo certo")
    void corridaNoEmail() {
        when(businessRepository.existsBySlug(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "erro", new RuntimeException(
                                "duplicate key value violates unique constraint "
                                        + "\"app_user_email_unique\"")));

        assertThatThrownBy(() -> handler.register(comando()))
                .isInstanceOf(EmailAlreadyUsedException.class);
    }

    @Test
    @DisplayName("violação que não sabemos traduzir propaga — é defeito, não regra")
    void violacaoDesconhecidaPropaga() {
        when(businessRepository.existsBySlug(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(businessRepository.saveAndFlush(any(Business.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "erro", new RuntimeException("constraint_que_ninguem_conhece")));

        assertThatThrownBy(() -> handler.register(comando()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("normaliza slug e e-mail antes de consultar")
    void normalizaEntrada() {
        when(businessRepository.existsBySlug("barbearia-do-joao")).thenReturn(false);
        when(userRepository.existsByEmail("joao@exemplo.com")).thenReturn(false);

        handler.register(new RegisterBusinessCommand(
                "Barbearia do João", "  BARBEARIA-DO-JOAO  ", "  Joao@Exemplo.COM  ",
                "senha-do-joao"));

        verify(businessRepository).existsBySlug("barbearia-do-joao");
        verify(userRepository).existsByEmail("joao@exemplo.com");
    }

    @Test
    @DisplayName("senha curta é recusada na construção do comando, antes de chegar ao handler")
    void senhaCurta() {
        assertThatThrownBy(() -> new RegisterBusinessCommand(
                        "Barbearia", "barbearia", "joao@exemplo.com", "curta"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8 caracteres");
    }

    @Test
    @DisplayName("o comando não expõe a senha no toString")
    void comandoNaoVazaSenha() {
        assertThat(comando().toString()).doesNotContain("senha-do-joao");
    }
}
