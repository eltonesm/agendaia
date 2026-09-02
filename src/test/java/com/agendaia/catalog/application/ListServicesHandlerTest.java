package com.agendaia.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.agendaia.catalog.domain.Service;
import com.agendaia.catalog.domain.ServiceRepository;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso da listagem de serviços, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class ListServicesHandlerTest {

    @Mock private ServiceRepository serviceRepository;

    private ListServicesHandler handler;

    @BeforeEach
    void montar() {
        handler = new ListServicesHandler(serviceRepository);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("lista os serviços do tenant do contexto")
    void listaDoTenantDoContexto() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        var servico = Service.register(tenant, "Corte de Cabelo", null);
        when(serviceRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenant.value()))
                .thenReturn(List.of(servico));

        var lista = handler.list();

        assertThat(lista).hasSize(1);
        assertThat(lista.getFirst().id()).isEqualTo(servico.id());
        assertThat(lista.getFirst().name()).isEqualTo("Corte de Cabelo");
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de devolver a lista de alguém")
    void semTenantRecusa() {
        assertThatThrownBy(handler::list)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");
    }

    @Test
    @DisplayName("estabelecimento sem servico devolve lista vazia, nao erro")
    void semServicoDevolveListaVazia() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(serviceRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenant.value()))
                .thenReturn(List.of());

        assertThat(handler.list()).isEmpty();
    }
}
