package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.agendaia.organization.application.command.RegisterBusinessOperatingHoursCommand;
import com.agendaia.organization.application.port.out.BusinessOperatingHoursRepository;
import com.agendaia.organization.domain.BusinessOperatingHours;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso do cadastro de horário de funcionamento, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class RegisterBusinessOperatingHoursHandlerTest {

    @Mock private BusinessOperatingHoursRepository businessOperatingHoursRepository;

    private RegisterBusinessOperatingHoursHandler handler;

    @BeforeEach
    void montar() {
        handler = new RegisterBusinessOperatingHoursHandler(businessOperatingHoursRepository);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private RegisterBusinessOperatingHoursCommand comando() {
        return new RegisterBusinessOperatingHoursCommand(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(18, 0));
    }

    @Test
    @DisplayName("o tenant gravado vem do TenantContext, nunca de argumento")
    void tenantVemDoContexto() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);

        handler.register(comando());

        var captor = ArgumentCaptor.forClass(BusinessOperatingHours.class);
        verify(businessOperatingHoursRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenant);
    }

    @Test
    @DisplayName("devolve o id da faixa cadastrada")
    void devolveOCadastrado() {
        TenantContext.set(TenantId.of(UuidV7.generate()));

        var resultado = handler.register(comando());

        assertThat(resultado.id()).isNotNull();
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de gravar para ninguém")
    void semTenantRecusa() {
        assertThatThrownBy(() -> handler.register(comando()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");

        verify(businessOperatingHoursRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("intervalo invalido recusa antes de gravar")
    void intervaloInvalidoRecusa() {
        TenantContext.set(TenantId.of(UuidV7.generate()));
        var invalido = new RegisterBusinessOperatingHoursCommand(DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(8, 0));

        assertThatThrownBy(() -> handler.register(invalido)).isInstanceOf(IllegalArgumentException.class);

        verify(businessOperatingHoursRepository, never()).saveAndFlush(any());
    }
}
