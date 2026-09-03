package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.organization.application.command.RegisterTimeOffCommand;
import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.TimeOffRepository;
import com.agendaia.organization.domain.TimeOff;
import com.agendaia.organization.domain.exception.ProfessionalNotFoundException;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso do cadastro de bloqueio, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class RegisterTimeOffHandlerTest {

    @Mock private TimeOffRepository timeOffRepository;
    @Mock private ProfessionalRepository professionalRepository;

    private RegisterTimeOffHandler handler;

    private final UUID professionalId = UuidV7.generate();
    private final Instant inicio = Instant.now();
    private final Instant fim = inicio.plus(1, ChronoUnit.DAYS);

    @BeforeEach
    void montar() {
        handler = new RegisterTimeOffHandler(timeOffRepository, professionalRepository);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("cadastra bloqueio de um profissional que pertence ao tenant")
    void cadastraBloqueioDeProfissional() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(professionalRepository.existsByIdAndTenantId(professionalId, tenant.value())).thenReturn(true);

        var resultado = handler.register(new RegisterTimeOffCommand(professionalId, inicio, fim, "Consulta"));

        assertThat(resultado.id()).isNotNull();
        var captor = ArgumentCaptor.forClass(TimeOff.class);
        verify(timeOffRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenant);
    }

    @Test
    @DisplayName("cadastra bloqueio sem profissional (estabelecimento inteiro) sem validar profissional nenhum")
    void cadastraBloqueioDoEstabelecimentoInteiro() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);

        var resultado = handler.register(new RegisterTimeOffCommand(null, inicio, fim, "Feriado"));

        assertThat(resultado.id()).isNotNull();
        verify(professionalRepository, never()).existsByIdAndTenantId(any(), any());
    }

    @Test
    @DisplayName("recusa profissional que nao pertence ao tenant da sessao (BR-8)")
    void recusaProfissionalDeOutroTenant() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(professionalRepository.existsByIdAndTenantId(professionalId, tenant.value())).thenReturn(false);

        assertThatThrownBy(() -> handler.register(new RegisterTimeOffCommand(professionalId, inicio, fim, null)))
                .isInstanceOf(ProfessionalNotFoundException.class);

        verify(timeOffRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de gravar para ninguém")
    void semTenantRecusa() {
        assertThatThrownBy(() -> handler.register(new RegisterTimeOffCommand(null, inicio, fim, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");

        verify(timeOffRepository, never()).saveAndFlush(any());
    }
}
