package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.agendaia.organization.application.command.RegisterProfessionalCommand;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.ProfessionalRepository;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso do cadastro de profissional, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class RegisterProfessionalHandlerTest {

    @Mock private ProfessionalRepository professionalRepository;

    private RegisterProfessionalHandler handler;

    @BeforeEach
    void montar() {
        handler = new RegisterProfessionalHandler(professionalRepository);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("o tenant gravado vem do TenantContext, nunca de argumento")
    void tenantVemDoContexto() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);

        handler.register(new RegisterProfessionalCommand("João da Silva"));

        var captor = ArgumentCaptor.forClass(Professional.class);
        verify(professionalRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenant);
    }

    @Test
    @DisplayName("devolve id e nome do profissional cadastrado")
    void devolveOCadastrado() {
        TenantContext.set(TenantId.of(UuidV7.generate()));

        var resultado = handler.register(new RegisterProfessionalCommand("João da Silva"));

        assertThat(resultado.id()).isNotNull();
        assertThat(resultado.name()).isEqualTo("João da Silva");
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de gravar para ninguém")
    void semTenantRecusa() {
        assertThatThrownBy(() -> handler.register(new RegisterProfessionalCommand("João da Silva")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");

        verify(professionalRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("nome invalido recusa antes de gravar")
    void nomeInvalidoRecusa() {
        TenantContext.set(TenantId.of(UuidV7.generate()));

        assertThatThrownBy(() -> handler.register(new RegisterProfessionalCommand("")))
                .isInstanceOf(IllegalArgumentException.class);

        verify(professionalRepository, never()).saveAndFlush(any());
    }
}
