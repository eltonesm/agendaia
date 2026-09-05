package com.agendaia.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.billing.application.port.out.BillingAccountRepository;
import com.agendaia.billing.domain.AccessStatus;
import com.agendaia.billing.domain.BillingAccount;
import com.agendaia.organization.api.BusinessDirectory;
import com.agendaia.organization.api.BusinessRef;
import com.agendaia.shared.UuidV7;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso de billing, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class BillingAccountServiceTest {

    private static final UUID TENANT_ID = UuidV7.generate();

    @Mock private BillingAccountRepository billingAccountRepository;
    @Mock private BusinessDirectory businessDirectory;

    private BillingAccountService service;

    @BeforeEach
    void montar() {
        service = new BillingAccountService(billingAccountRepository, businessDirectory);
    }

    private BusinessRef negocio(UUID tenantId) {
        return new BusinessRef(tenantId, "Barbearia do João", "barbearia-do-joao", Instant.now());
    }

    @Test
    @DisplayName("statusFor cria a conta sob demanda quando ainda não existe (BR-8), gravando uma única vez")
    void statusForCriaContaQuandoNaoExiste() {
        when(billingAccountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        when(businessDirectory.find(TENANT_ID)).thenReturn(Optional.of(negocio(TENANT_ID)));
        when(billingAccountRepository.saveAndFlush(any(BillingAccount.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        var status = service.statusFor(TENANT_ID);

        assertThat(status).isEqualTo(AccessStatus.TRIAL);
        verify(billingAccountRepository, times(1)).saveAndFlush(any(BillingAccount.class));
    }

    @Test
    @DisplayName("statusFor não grava de novo quando a conta já existe — get-or-create não duplica")
    void statusForNaoDuplicaContaExistente() {
        var registeredOn = LocalDate.now().minusDays(10);
        var contaExistente = BillingAccount.startTrial(TENANT_ID, registeredOn);
        when(billingAccountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(contaExistente));

        service.statusFor(TENANT_ID);

        verify(billingAccountRepository, never()).saveAndFlush(any());
        verify(businessDirectory, never()).find(any());
    }

    @Test
    @DisplayName("snapshotFor consulta o repositório uma única vez e devolve status + fim da carência juntos")
    void snapshotForUmaUnicaConsulta() {
        var registeredOn = LocalDate.now().minusDays(40);
        var contaVencida = BillingAccount.startTrial(TENANT_ID, registeredOn);
        when(billingAccountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(contaVencida));

        var snapshot = service.snapshotFor(TENANT_ID);

        assertThat(snapshot.graceEndsAt()).isEqualTo(contaVencida.graceEndsOn());
        verify(billingAccountRepository, times(1)).findByTenantId(TENANT_ID);
    }

    @Test
    @DisplayName("extendUntil recusa data não futura, delegando ao domínio, sem gravar")
    void extendUntilRecusaDataNaoFutura() {
        var contaExistente = BillingAccount.startTrial(TENANT_ID, LocalDate.now().minusDays(5));
        when(billingAccountRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(contaExistente));

        assertThatThrownBy(() -> service.extendUntil(TENANT_ID, LocalDate.now().minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(billingAccountRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("listForOperator busca as contas em lote (findByTenantIdIn), nunca uma consulta por estabelecimento")
    void listForOperatorBuscaEmLote() {
        var negocio1 = negocio(UuidV7.generate());
        var negocio2 = negocio(UuidV7.generate());
        var negocio3 = negocio(UuidV7.generate());
        when(businessDirectory.listAll()).thenReturn(List.of(negocio1, negocio2, negocio3));
        when(billingAccountRepository.findByTenantIdIn(anyCollection())).thenReturn(List.of());
        when(billingAccountRepository.saveAndFlush(any(BillingAccount.class)))
                .thenAnswer(chamada -> chamada.getArgument(0));

        var resultado = service.listForOperator();

        assertThat(resultado).hasSize(3);
        verify(billingAccountRepository, times(1)).findByTenantIdIn(anyCollection());
        verify(billingAccountRepository, never()).findByTenantId(any());
    }

    @Test
    @DisplayName("listForOperator reaproveita a conta já existente, sem criar de novo, e reflete o status calculado")
    void listForOperatorReaproveitaContaExistente() {
        var negocio = negocio(TENANT_ID);
        var registeredOn = negocio.createdAt().atZone(ZoneId.systemDefault()).toLocalDate();
        var contaExistente = BillingAccount.startTrial(TENANT_ID, registeredOn);
        when(businessDirectory.listAll()).thenReturn(List.of(negocio));
        when(billingAccountRepository.findByTenantIdIn(anyCollection())).thenReturn(List.of(contaExistente));

        var resultado = service.listForOperator();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).status()).isEqualTo(AccessStatus.TRIAL);
        assertThat(resultado.get(0).accessValidUntil()).isEqualTo(contaExistente.accessValidUntil());
        verify(billingAccountRepository, never()).saveAndFlush(any());
    }
}
