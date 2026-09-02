package com.agendaia.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.catalog.application.command.RegisterServiceCommand;
import com.agendaia.catalog.domain.Service;
import com.agendaia.catalog.domain.ServiceRepository;
import com.agendaia.catalog.domain.exception.ServiceNameAlreadyUsedException;
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

/** Caso de uso do cadastro de serviço, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class RegisterServiceHandlerTest {

    @Mock private ServiceRepository serviceRepository;

    private RegisterServiceHandler handler;

    @BeforeEach
    void montar() {
        handler = new RegisterServiceHandler(serviceRepository);
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

        handler.register(new RegisterServiceCommand("Corte de Cabelo", null));

        var captor = ArgumentCaptor.forClass(Service.class);
        verify(serviceRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenant);
    }

    @Test
    @DisplayName("devolve id e nome do serviço cadastrado")
    void devolveOCadastrado() {
        TenantContext.set(TenantId.of(UuidV7.generate()));

        var resultado = handler.register(new RegisterServiceCommand("Corte de Cabelo", null));

        assertThat(resultado.id()).isNotNull();
        assertThat(resultado.name()).isEqualTo("Corte de Cabelo");
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de gravar para ninguém")
    void semTenantRecusa() {
        assertThatThrownBy(() -> handler.register(new RegisterServiceCommand("Corte de Cabelo", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");

        verify(serviceRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("nome invalido recusa antes de gravar")
    void nomeInvalidoRecusa() {
        TenantContext.set(TenantId.of(UuidV7.generate()));

        assertThatThrownBy(() -> handler.register(new RegisterServiceCommand("", null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(serviceRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("nome duplicado no mesmo tenant recusa antes de gravar (BR-1)")
    void nomeDuplicadoRecusa() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(serviceRepository.existsByTenantIdAndName(tenant.value(), "Corte de Cabelo")).thenReturn(true);

        assertThatThrownBy(() -> handler.register(new RegisterServiceCommand("Corte de Cabelo", null)))
                .isInstanceOf(ServiceNameAlreadyUsedException.class);

        verify(serviceRepository, never()).saveAndFlush(any());
    }
}
