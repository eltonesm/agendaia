package com.agendaia.platform.tenant;

import com.agendaia.platform.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Popula o {@link TenantContext} a partir da sessão autenticada.
 *
 * <p>Roda depois do filtro de autenticação do Spring Security — por isso a
 * ordem baixa não serve aqui; o registro como {@code @Component} coloca este
 * filtro após a cadeia de segurança, que é onde o principal já existe.
 *
 * <p>Também coloca {@code tenantId} no MDC, para que toda linha de log da
 * requisição saia identificada. Num sistema multi-tenant, "está lento" é a
 * pergunta errada: a certa é "para qual estabelecimento" (TODO-108).
 */
@Component
@Order(Integer.MIN_VALUE + 100)
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
