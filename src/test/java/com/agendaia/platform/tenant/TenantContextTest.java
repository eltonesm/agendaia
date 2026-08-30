package com.agendaia.platform.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.TenantId;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Teste puro, sem Spring: o TenantContext é a fronteira de segurança do produto
 * e precisa ser verificável em milissegundos.
 */
class TenantContextTest {

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("guarda e devolve o tenant da thread atual")
    void guardaEDevolve() {
        var tenant = TenantId.of(UUID.randomUUID());

        TenantContext.set(tenant);

        assertThat(TenantContext.current()).contains(tenant);
        assertThat(TenantContext.require()).isEqualTo(tenant);
    }

    @Test
    @DisplayName("vem vazio antes de qualquer resolução")
    void vazioPorPadrao() {
        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    @DisplayName("require falha em vez de deixar consultar sem recorte")
    void requireFalhaSemTenant() {
        assertThatThrownBy(TenantContext::require)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Nenhum tenant no contexto");
    }

    @Test
    @DisplayName("clear apaga — sem isso a thread do pool herdaria o tenant anterior")
    void clearApaga() {
        TenantContext.set(TenantId.of(UUID.randomUUID()));

        TenantContext.clear();

        assertThat(TenantContext.current()).isEmpty();
    }

    @Test
    @DisplayName("o tenant de uma thread não vaza para outra")
    void naoVazaEntreThreads() throws Exception {
        var daPrincipal = TenantId.of(UUID.randomUUID());
        TenantContext.set(daPrincipal);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var naOutraThread = executor.submit(TenantContext::current);

            assertThat(naOutraThread.get(5, TimeUnit.SECONDS))
                    .as("outra thread não pode enxergar o tenant desta")
                    .isEmpty();
        }

        assertThat(TenantContext.current()).contains(daPrincipal);
    }

    @Test
    @DisplayName("recusa tenant nulo em vez de guardar ausência silenciosa")
    void recusaNulo() {
        assertThatThrownBy(() -> TenantContext.set(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
