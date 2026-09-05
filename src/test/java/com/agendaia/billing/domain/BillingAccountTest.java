package com.agendaia.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.UuidV7;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring e sem banco, mesmo sendo entidade JPA. */
class BillingAccountTest {

    private static final UUID TENANT_ID = UuidV7.generate();
    private static final LocalDate CADASTRO = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIM_DO_TESTE = CADASTRO.plusDays(30);

    @Test
    @DisplayName("startTrial: trialEndsAt e accessValidUntil nascem iguais, 30 dias após o cadastro (BR-1)")
    void startTrialInicia30DiasDeTeste() {
        var conta = BillingAccount.startTrial(TENANT_ID, CADASTRO);

        assertThat(conta.trialEndsAt()).isEqualTo(FIM_DO_TESTE);
        assertThat(conta.accessValidUntil()).isEqualTo(FIM_DO_TESTE);
        assertThat(conta.tenantId().value()).isEqualTo(TENANT_ID);
    }

    @Test
    @DisplayName("startTrial recusa nascer sem estabelecimento")
    void startTrialRecusaSemTenant() {
        assertThatThrownBy(() -> BillingAccount.startTrial(null, CADASTRO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("estabelecimento");
    }

    @Test
    @DisplayName("startTrial recusa nascer sem data de cadastro")
    void startTrialRecusaSemDataDeCadastro() {
        assertThatThrownBy(() -> BillingAccount.startTrial(TENANT_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cadastro");
    }

    @Test
    @DisplayName("extendUntil recusa data que não seja estritamente futura (BR-3)")
    void extendUntilRecusaDataNaoFutura() {
        var conta = BillingAccount.startTrial(TENANT_ID, CADASTRO);
        var hoje = LocalDate.of(2026, 6, 1);

        assertThatThrownBy(() -> conta.extendUntil(hoje, hoje))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posterior a hoje");
        assertThatThrownBy(() -> conta.extendUntil(hoje.minusDays(1), hoje))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> conta.extendUntil(null, hoje))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("extendUntil aceita data estritamente futura e passa a valer como novo accessValidUntil")
    void extendUntilAceitaDataFutura() {
        var conta = BillingAccount.startTrial(TENANT_ID, CADASTRO);
        var hoje = LocalDate.of(2026, 6, 1);
        var novaData = hoje.plusDays(30);

        conta.extendUntil(novaData, hoje);

        assertThat(conta.accessValidUntil()).isEqualTo(novaData);
    }

    @Test
    @DisplayName("statusOn: nunca estendido além do trial -> TRIAL, mesmo no último dia (BR-2/DD-6)")
    void statusOnTrialAteOUltimoDia() {
        var conta = BillingAccount.startTrial(TENANT_ID, CADASTRO);

        assertThat(conta.statusOn(FIM_DO_TESTE)).isEqualTo(AccessStatus.TRIAL);
        assertThat(conta.statusOn(FIM_DO_TESTE.minusDays(1))).isEqualTo(AccessStatus.TRIAL);
    }

    @Test
    @DisplayName("statusOn: accessValidUntil estendido além de trialEndsAt -> PAID, prova de pagamento (BR-2/DD-6)")
    void statusOnPagoQuandoEstendidoAlemDoTrial() {
        var conta = BillingAccount.startTrial(TENANT_ID, CADASTRO);
        var hoje = FIM_DO_TESTE.minusDays(5);
        var novaData = FIM_DO_TESTE.plusDays(30);
        conta.extendUntil(novaData, hoje);

        assertThat(conta.statusOn(hoje)).isEqualTo(AccessStatus.PAID);
        assertThat(conta.statusOn(novaData)).isEqualTo(AccessStatus.PAID);
    }

    @Test
    @DisplayName("statusOn: um dia após o vencimento entra em carência (BR-2/BR-4)")
    void statusOnCarenciaNoDiaSeguinteAoVencimento() {
        var conta = BillingAccount.startTrial(TENANT_ID, CADASTRO);

        assertThat(conta.statusOn(FIM_DO_TESTE.plusDays(1))).isEqualTo(AccessStatus.GRACE_PERIOD);
    }

    @Test
    @DisplayName("statusOn: no último dia exato da carência (accessValidUntil + 5) ainda é GRACE_PERIOD (BR-4)")
    void statusOnUltimoDiaDaCarenciaAindaLibera() {
        var conta = BillingAccount.startTrial(TENANT_ID, CADASTRO);

        assertThat(conta.graceEndsOn()).isEqualTo(FIM_DO_TESTE.plusDays(5));
        assertThat(conta.statusOn(conta.graceEndsOn())).isEqualTo(AccessStatus.GRACE_PERIOD);
    }

    @Test
    @DisplayName("statusOn: um dia após o fim da carência (accessValidUntil + 6) bloqueia (BR-4)")
    void statusOnBloqueiaAposFimDaCarencia() {
        var conta = BillingAccount.startTrial(TENANT_ID, CADASTRO);

        assertThat(conta.statusOn(FIM_DO_TESTE.plusDays(6))).isEqualTo(AccessStatus.BLOCKED);
    }
}
