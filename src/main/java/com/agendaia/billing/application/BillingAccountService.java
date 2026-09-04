package com.agendaia.billing.application;

import com.agendaia.billing.application.port.out.BillingAccountRepository;
import com.agendaia.billing.domain.AccessStatus;
import com.agendaia.billing.domain.BillingAccount;
import com.agendaia.organization.api.BusinessDirectory;
import com.agendaia.organization.api.BusinessRef;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Get-or-create de {@link BillingAccount} por tenant (DD-2), cálculo de
 * status (BR-2) e a ação de marcar prazo (BR-3/DD-7).
 *
 * <p>{@link #statusFor} é chamado por {@code AccessGuardFilter} a cada
 * requisição a {@code /admin/**} — depois da primeira vez (quando a conta
 * é criada), toda chamada seguinte é uma única consulta indexada por
 * {@code tenantId}, sem tocar {@code organization.api}.
 */
@Service
public class BillingAccountService {

    private final BillingAccountRepository billingAccountRepository;
    private final BusinessDirectory businessDirectory;

    public BillingAccountService(BillingAccountRepository billingAccountRepository, BusinessDirectory businessDirectory) {
        this.billingAccountRepository = billingAccountRepository;
        this.businessDirectory = businessDirectory;
    }

    @Transactional
    public AccessStatus statusFor(UUID tenantId) {
        return getOrCreate(tenantId).statusOn(LocalDate.now());
    }

    /** BR-3/DD-7: marcar pagamento e estender prazo são a mesma ação. */
    @Transactional
    public void extendUntil(UUID tenantId, LocalDate newDate) {
        getOrCreate(tenantId).extendUntil(newDate);
    }

    /**
     * Painel do operador (US-2). Busca todos os estabelecimentos de uma vez
     * (DD-5) e todas as contas de cobrança correspondentes em lote — sem
     * uma consulta por estabelecimento.
     */
    @Transactional
    public List<EstablishmentView> listForOperator() {
        var estabelecimentos = businessDirectory.listAll();
        var tenantIds = estabelecimentos.stream().map(BusinessRef::tenantId).toList();
        var contasPorTenant = billingAccountRepository.findByTenantIdIn(tenantIds).stream()
                .collect(Collectors.toMap(conta -> conta.tenantId().value(), Function.identity()));

        var hoje = LocalDate.now();
        return estabelecimentos.stream()
                .map(negocio -> {
                    var conta = contasPorTenant.computeIfAbsent(negocio.tenantId(), id -> criarConta(negocio));
                    return new EstablishmentView(
                            negocio.tenantId(),
                            negocio.name(),
                            negocio.slug(),
                            negocio.createdAt(),
                            conta.statusOn(hoje),
                            conta.accessValidUntil());
                })
                .toList();
    }

    private BillingAccount getOrCreate(UUID tenantId) {
        return billingAccountRepository.findByTenantId(tenantId).orElseGet(() -> {
            var negocio = businessDirectory
                    .find(tenantId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Estabelecimento não encontrado para conta de cobrança: tenantId=" + tenantId));
            return criarConta(negocio);
        });
    }

    private BillingAccount criarConta(BusinessRef negocio) {
        var registeredOn = negocio.createdAt().atZone(ZoneId.systemDefault()).toLocalDate();
        var nova = BillingAccount.startTrial(negocio.tenantId(), registeredOn);
        return billingAccountRepository.saveAndFlush(nova);
    }
}
