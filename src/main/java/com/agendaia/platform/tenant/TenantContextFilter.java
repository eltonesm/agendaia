package com.agendaia.platform.tenant;

import com.agendaia.platform.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Popula o {@link TenantContext} a partir da sessão autenticada.
 *
 * <p><strong>A ordem é o ponto todo.</strong> A cadeia do Spring Security é um
 * único filtro no chain do servlet, em {@link SecurityFilterProperties#DEFAULT_FILTER_ORDER}
 * ({@code -100}). Quem vem antes dele lê um {@code SecurityContextHolder} ainda
 * vazio; quem vem depois roda dentro dele, com o principal já resolvido. Este
 * filtro precisa do principal, logo vem depois — por um ponto só.
 *
 * <p>Também coloca {@code tenantId} no MDC, para que toda linha de log da
 * requisição saia identificada. Num sistema multi-tenant, "está lento" é a
 * pergunta errada: a certa é "para qual estabelecimento" (TODO-108).
 */
@Component
@Order(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1)
public class TenantContextFilter extends OncePerRequestFilter {

    static final String MDC_TENANT = "tenantId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            resolverDaSessao();
            chain.doFilter(request, response);
        } finally {
            // Obrigatório mesmo quando a requisição falha: a thread volta para o
            // pool, e sem a limpeza a próxima requisição herdaria este tenant.
            TenantContext.clear();
            MDC.remove(MDC_TENANT);
        }
    }

    private void resolverDaSessao() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUser usuario) {
            TenantContext.set(usuario.tenantId());
            MDC.put(MDC_TENANT, usuario.tenantId().toString());
        }
    }
}
