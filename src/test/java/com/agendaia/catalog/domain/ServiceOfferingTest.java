package com.agendaia.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.Money;
import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Sem Spring e sem banco, mesmo sendo entidade JPA. */
class ServiceOfferingTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());
    private static final UUID SERVICE_ID = UuidV7.generate();
    private static final UUID PROFESSIONAL_ID = UuidV7.generate();
    private static final Money PRECO = Money.reais(new BigDecimal("30.00"));

    @Test
    @DisplayName("nasce ativa, com id UUIDv7 e os dados informados")
    void nasceAtiva() {
        var oferta = ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, 30, PRECO, 10);

        assertThat(oferta.id()).isNotNull();
        assertThat(oferta.id().version()).isEqualTo(7);
        assertThat(oferta.isActive()).isTrue();
        assertThat(oferta.tenantId()).isEqualTo(TENANT);
        assertThat(oferta.serviceId()).isEqualTo(SERVICE_ID);
        assertThat(oferta.professionalId()).isEqualTo(PROFESSIONAL_ID);
        assertThat(oferta.durationMinutes()).isEqualTo(30);
        assertThat(oferta.price()).isEqualTo(PRECO);
        assertThat(oferta.bufferMinutes()).isEqualTo(10);
    }

    @Test
    @DisplayName("aceita duração que não é múltiplo de 10 (ADR 0006: grade fixa é do slot, não da duração)")
    void duracaoNaoPrecisaSerMultiploDeDez() {
        var oferta = ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, 45, PRECO, 0);

        assertThat(oferta.durationMinutes()).isEqualTo(45);
    }

    @Test
    @DisplayName("buffer zero é aceito")
    void bufferZeroEAceito() {
        var oferta = ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, 30, PRECO, 0);

        assertThat(oferta.bufferMinutes()).isZero();
    }

    @Test
    @DisplayName("recusa buffer negativo")
    void recusaBufferNegativo() {
        assertThatThrownBy(() -> ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, 30, PRECO, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negativo");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -30})
    @DisplayName("recusa duração zero ou negativa")
    void recusaDuracaoNaoPositiva(int duracao) {
        assertThatThrownBy(() -> ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, duracao, PRECO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    @DisplayName("recusa nascer sem estabelecimento")
    void recusaSemTenant() {
        assertThatThrownBy(() -> ServiceOffering.register(null, SERVICE_ID, PROFESSIONAL_ID, 30, PRECO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    @DisplayName("recusa nascer sem serviço")
    void recusaSemServico() {
        assertThatThrownBy(() -> ServiceOffering.register(TENANT, null, PROFESSIONAL_ID, 30, PRECO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviço");
    }

    @Test
    @DisplayName("recusa nascer sem profissional")
    void recusaSemProfissional() {
        assertThatThrownBy(() -> ServiceOffering.register(TENANT, SERVICE_ID, null, 30, PRECO, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("profissional");
    }

    @Test
    @DisplayName("recusa nascer sem preço")
    void recusaSemPreco() {
        assertThatThrownBy(() -> ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, 30, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preço");
    }

    @Test
    @DisplayName("price() devolve Money, não long cru")
    void priceDevolveMoney() {
        var oferta = ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, 30, PRECO, 0);

        assertThat(oferta.price()).isInstanceOf(Money.class);
        assertThat(oferta.price().format()).isEqualTo("R$ 30,00");
    }

    @Test
    @DisplayName("desativar não apaga — o histórico continua íntegro (ADR 0011)")
    void desativarNaoApaga() {
        var oferta = ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, 30, PRECO, 0);

        oferta.deactivate();

        assertThat(oferta.isActive()).isFalse();
        assertThat(oferta.id()).isNotNull();
    }

    @Test
    @DisplayName("igualdade por identidade")
    void igualdadePorId() {
        var oferta = ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, 30, PRECO, 0);

        assertThat(oferta).isEqualTo(oferta);
        assertThat(oferta)
                .isNotEqualTo(ServiceOffering.register(TENANT, SERVICE_ID, PROFESSIONAL_ID, 30, PRECO, 0));
    }
}
