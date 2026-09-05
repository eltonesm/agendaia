package com.agendaia.billing.adapter.in.web;

import com.agendaia.billing.application.BillingAccountService;
import com.agendaia.billing.application.BillingAccountService.AccessSnapshot;
import com.agendaia.billing.domain.AccessStatus;
import com.agendaia.platform.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bloqueia {@code /admin/**} quando o estabelecimento venceu a carência
 * (BR-4). Mora em {@code billing}, não em {@code platform}: decidir "está
 * bloqueado" é regra de negócio, e {@code platform} proíbe isso no próprio
 * {@code package-info.java} (DD-4 da spec técnica).
 *
 * <p>Roda depois de {@code TenantContextFilter} (precisa do
 * {@link TenantContext} já resolvido) e guarda o resultado como atributo
 * de requisição para {@link BillingBannerAdvice} reaproveitar, sem
 * consultar o banco de novo na mesma requisição.
 *
 * <p><strong>Falha aberta, não fechada</strong> — o oposto de
 * {@code TenantContext.require()}. Um erro inesperado ao calcular o
 * status deixa a requisição seguir, com log em {@code WARN}: bloquear por
 * engano quem pagou é pior que deixar passar por alguns minutos durante um
 * defeito do sistema (Security, spec técnica).
 *
 * <p>{@code BillingAccountService} chega por {@link ObjectProvider}: este
 * filtro é {@code @Component} e por isso entra também em fatias
 * {@code @WebMvcTest} de outros contextos, que não sobem {@code billing}.
 * Sem bean disponível, a mesma filosofia de falha aberta se aplica — segue
 * sem bloquear em vez de derrubar o contexto de teste.
 */
@Component
@Order(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 2)
public class AccessGuardFilter extends OncePerRequestFilter {

    static final String REQUEST_ATTR_SNAPSHOT = "billingAccessSnapshot";
    private static final String ADMIN_PREFIX = "/admin";
    private static final String SUSPENDED_PATH = "/admin/conta-suspensa";

    private static final Logger log = LoggerFactory.getLogger(AccessGuardFilter.class);

    private final ObjectProvider<BillingAccountService> billingAccountServiceProvider;

    public AccessGuardFilter(ObjectProvider<BillingAccountService> billingAccountServiceProvider) {
        this.billingAccountServiceProvider = billingAccountServiceProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var path = pathWithinApp(request);

        if (!path.startsWith(ADMIN_PREFIX) || path.equals(SUSPENDED_PATH)) {
            chain.doFilter(request, response);
            return;
        }

        var tenantId = TenantContext.current();
        if (tenantId.isEmpty()) {
            // Rota /admin sem tenant resolvido não é problema desta feature
            // — quem recusa é a cadeia de autenticação, antes deste filtro
            // ter algo a decidir.
            chain.doFilter(request, response);
            return;
        }

        var billingAccountService = billingAccountServiceProvider.getIfAvailable();
        if (billingAccountService != null) {
            try {
                var snapshot = billingAccountService.snapshotFor(tenantId.get().value());
                request.setAttribute(REQUEST_ATTR_SNAPSHOT, snapshot);
                if (snapshot.status() == AccessStatus.BLOCKED) {
                    response.sendRedirect(request.getContextPath() + SUSPENDED_PATH);
                    return;
                }
            } catch (RuntimeException e) {
                log.warn("Falha ao calcular status de acesso de billing — seguindo sem bloquear", e);
            }
        }

        chain.doFilter(request, response);
    }

    private static String pathWithinApp(HttpServletRequest request) {
        var contextPath = request.getContextPath();
        var uri = request.getRequestURI();
        return contextPath.isEmpty() ? uri : uri.substring(contextPath.length());
    }

    static AccessSnapshot snapshotFrom(HttpServletRequest request) {
        return (AccessSnapshot) request.getAttribute(REQUEST_ATTR_SNAPSHOT);
    }
}
