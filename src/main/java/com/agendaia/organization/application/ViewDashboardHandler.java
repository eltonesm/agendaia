package com.agendaia.organization.application;

import com.agendaia.organization.application.port.in.DashboardView;
import com.agendaia.organization.application.port.in.ViewDashboardUseCase;
import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.platform.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta o painel do estabelecimento da sessão.
 *
 * <p>O tenant vem do {@link TenantContext}, nunca de argumento. É a diferença
 * entre "mostre meu painel" e "mostre o painel deste id" — a segunda forma é
 * uma falha de isolamento esperando acontecer.
 */
@Service
public class ViewDashboardHandler implements ViewDashboardUseCase {

    private final BusinessRepository businessRepository;
    private final String publicBaseUrl;

    public ViewDashboardHandler(
            BusinessRepository businessRepository,
            @Value("${agendaia.public-base-url}") String publicBaseUrl) {
        this.businessRepository = businessRepository;
        // Barra no fim é o jeito natural de escrever a variável de ambiente, e
        // produziria "https://agendaia.com//b/slug". O link que o dono manda
        // para os clientes é o produto — não pode depender de acertar isso.
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardView current() {
        var tenantId = TenantContext.require();

        var business = businessRepository
                .findById(tenantId.value())
                .orElseThrow(() -> new IllegalStateException(
                        "Sessão aponta para um estabelecimento que não existe: " + tenantId));

        return new DashboardView(
                business.name(), business.slug(), publicBaseUrl + "/b/" + business.slug());
    }
}
