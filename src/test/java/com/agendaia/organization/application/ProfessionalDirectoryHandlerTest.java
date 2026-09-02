package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.agendaia.organization.domain.Professional;
import com.agendaia.organization.domain.ProfessionalRepository;
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

/** Implementação de organization.api, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class ProfessionalDirectoryHandlerTest {

    @Mock private ProfessionalRepository professionalRepository;

    private ProfessionalDirectoryHandler handler;

    @BeforeEach
    void montar() {
        handler = new ProfessionalDirectoryHandler(professionalRepository);
    }

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("lista os profissionais ativos do tenant do contexto")
    void listaDoTenantDoContexto() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        var profissional = Professional.register(tenant, "João da Silva");
        when(professionalRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenant.value()))
                .thenReturn(List.of(profissional));

        var lista = handler.listActive();

        assertThat(lista).hasSize(1);
        assertThat(lista.getFirst().id()).isEqualTo(profissional.id());
        assertThat(lista.getFirst().name()).isEqualTo("João da Silva");
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de devolver a lista de alguém")
    void semTenantRecusa() {
        assertThatThrownBy(handler::listActive)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");
    }

    @Test
    @DisplayName("estabelecimento sem profissional ativo devolve lista vazia, nao erro")
    void semProfissionalDevolveListaVazia() {
        var tenant = TenantId.of(UuidV7.generate());
        TenantContext.set(tenant);
        when(professionalRepository.findByTenantIdAndActiveTrueOrderByNameAsc(tenant.value()))
                .thenReturn(List.of());

        assertThat(handler.listActive()).isEmpty();
    }
}
