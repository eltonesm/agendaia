package com.agendaia.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.platform.tenant.TenantContext;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso do painel, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class ViewDashboardHandlerTest {

    @Mock private BusinessRepository businessRepository;

    @AfterEach
    void limparContexto() {
        TenantContext.clear();
    }

    private Business barbearia() {
        return Business.register("Barbearia do João", "barbearia-do-joao");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://agendaia.com", "https://agendaia.com/"})
    @DisplayName("o link público não ganha barra dupla por causa da configuração")
    void naoDuplicaABarra(String base) {
        var business = barbearia();
        TenantContext.set(business.tenantId());
        when(businessRepository.findById(business.tenantId().value()))
                .thenReturn(Optional.of(business));

        var painel = new ViewDashboardHandler(businessRepository, base).current();

        assertThat(painel.publicUrl()).isEqualTo("https://agendaia.com/b/barbearia-do-joao");
    }

    @Test
    @DisplayName("sem tenant no contexto, recusa em vez de devolver o painel de alguém")
    void semTenantRecusa() {
        var handler = new ViewDashboardHandler(businessRepository, "https://agendaia.com");

        assertThatThrownBy(handler::current)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant");
    }

    @Test
    @DisplayName("o tenant vem do contexto, nunca de argumento")
    void tenantVemDoContexto() {
        var business = barbearia();
        var outro = TenantId.of(UuidV7.generate());
        TenantContext.set(business.tenantId());
        when(businessRepository.findById(business.tenantId().value()))
                .thenReturn(Optional.of(business));

        var painel = new ViewDashboardHandler(businessRepository, "https://agendaia.com").current();

        // A assinatura de current() não aceita id nenhum — este teste existe
        // para que acrescentar um parâmetro quebre algo visível.
        assertThat(painel.businessName()).isEqualTo("Barbearia do João");
        assertThat(business.tenantId()).isNotEqualTo(outro);
    }
}
