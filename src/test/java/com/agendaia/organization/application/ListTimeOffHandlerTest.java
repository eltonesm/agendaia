package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.agendaia.organization.application.port.out.ProfessionalRepository;
import com.agendaia.organization.application.port.out.TimeOffRepository;
import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.TimeOff;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso da listagem de bloqueios, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class ListTimeOffHandlerTest {

    @Mock private TimeOffRepository timeOffRepository;
    @Mock private ProfessionalRepository professionalRepository;

    private ListTimeOffHandler handler;

    private final Instant inicio = Instant.now();
    private final Instant fim = inicio.plus(1, ChronoUnit.DAYS);

    @BeforeEach
    void montar() {
        handler = new ListTimeOffHandler(timeOffRepository, professionalRepository);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("resolve o nome do profissional quando o bloqueio e de um profissional especifico")
    void resolveNomeQuandoTemProfissional() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        var profissional = Professional.register(tenant, "João da Silva");
        var bloqueio = TimeOff.register(tenant, profissional.id(), inicio, fim, "Consulta");

        when(timeOffRepository.findByTenantIdAndActiveTrueOrderByStartsAtDesc(tenant.value()))
                .thenReturn(List.of(bloqueio));
        when(professionalRepository.findAllById(List.of(profissional.id()))).thenReturn(List.of(profissional));

        var lista = handler.list();

        assertThat(lista).hasSize(1);
        assertThat(lista.getFirst().professionalName()).isEqualTo("João da Silva");
    }

    @Test
    @DisplayName("professionalName e nulo quando o bloqueio vale para o estabelecimento inteiro (DD-3)")
    void professionalNameNuloParaBloqueioGeral() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        var feriado = TimeOff.register(tenant, null, inicio, fim, "Feriado");

        when(timeOffRepository.findByTenantIdAndActiveTrueOrderByStartsAtDesc(tenant.value()))
                .thenReturn(List.of(feriado));
        when(professionalRepository.findAllById(List.of())).thenReturn(List.of());

        var lista = handler.list();

        assertThat(lista).hasSize(1);
        assertThat(lista.getFirst().professionalName()).isNull();
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de devolver a lista de alguém")
    void semTenantRecusa() {
        assertThatThrownBy(handler::list)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");
    }
}
