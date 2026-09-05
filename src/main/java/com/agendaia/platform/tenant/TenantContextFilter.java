package com.agendaia.platform.tenant;

import com.agendaia.organization.api.BusinessDirectory;
import com.agendaia.platform.security.AuthenticatedUser;
import com.agendaia.shared.TenantId;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Popula o {@link TenantContext} — duas vias de resolução (ADR 0004): a
 * sessão autenticada (área administrativa) ou o slug da URL pública
 * (pagina-publica-agendamento, TODO-006, DD-3 da spec técnica).
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
 *
 * <p>{@code BusinessDirectory} chega por {@link ObjectProvider}: este
 * filtro é {@code @Component} global e por isso entra em fatias
 * {@code @WebMvcTest} de outros contextos que não sobem {@code
 * organization} — provider vazio simplesmente não resolve pelo slug,
 * mesmo padrão já usado em {@code AccessGuardFilter} (back-office-operador).
 *
 * <p>Slug não encontrado: segue sem tenant nenhum — quem decide "isso é
 * 404" é o controller da rota pública, não este filtro (DD-3).
 */
@Component
@Order(SecurityFilterProperties.DEFAULT_FILTER_ORDER + 1)
public class TenantContextFilter extends OncePerRequestFilter {

    static final String MDC_TENANT = "tenantId";

    /** Lido por {@code platform.web.PublicLayoutAdvice} — evita uma segunda consulta por findBySlug (DD-4). */
    public static final String REQUEST_ATTR_RESOLVED_BUSINESS = "resolvedBusiness";

    private static final String PUBLIC_PREFIX = "/b/";

    private final ObjectProvider<BusinessDirectory> businessDirectoryProvider;

    public TenantContextFilter(ObjectProvider<BusinessDirectory> businessDirectoryProvider) {
        this.businessDirectoryProvider = businessDirectoryProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            if (!resolverDaSessao()) {
                resolverDoSlug(request);
            }
            chain.doFilter(request, response);
        } finally {
            // Obrigatório mesmo quando a requisição falha: a thread volta para o
            // pool, e sem a limpeza a próxima requisição herdaria este tenant.
            TenantContext.clear();
            MDC.remove(MDC_TENANT);
        }
    }

    /** @return true se resolveu (sessão autenticada com {@link AuthenticatedUser}). */
    private boolean resolverDaSessao() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUser usuario) {
            TenantContext.set(usuario.tenantId());
            MDC.put(MDC_TENANT, usuario.tenantId().toString());
            return true;
        }
        return false;
    }

    private void resolverDoSlug(HttpServletRequest request) {
        var slug = extrairSlug(request);
        if (slug == null) {
            return;
        }
        var businessDirectory = businessDirectoryProvider.getIfAvailable();
        if (businessDirectory == null) {
            return;
        }
        businessDirectory.findBySlug(slug).ifPresent(negocio -> {
            var tenantId = TenantId.of(negocio.tenantId());
            TenantContext.set(tenantId);
            MDC.put(MDC_TENANT, tenantId.value().toString());
            request.setAttribute(REQUEST_ATTR_RESOLVED_BUSINESS, negocio);
        });
    }

    /** {@code /b/{slug}} ou {@code /b/{slug}/qualquer-coisa} — vazio se o path não bate com o prefixo público. */
    private static String extrairSlug(HttpServletRequest request) {
        var contextPath = request.getContextPath();
        var uri = request.getRequestURI();
        var path = contextPath.isEmpty() ? uri : uri.substring(contextPath.length());

        if (!path.startsWith(PUBLIC_PREFIX)) {
            return null;
        }
        var resto = path.substring(PUBLIC_PREFIX.length());
        var barra = resto.indexOf('/');
        var slug = barra == -1 ? resto : resto.substring(0, barra);
        return slug.isBlank() ? null : slug;
    }
}
