package com.agendaia.platform.web;

import com.agendaia.organization.api.BusinessRef;
import com.agendaia.platform.security.AuthenticatedUser;
import com.agendaia.platform.tenant.TenantContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Coloca no model o que toda tela exibe como nome do estabelecimento.
 *
 * <p>Existe para o template não precisar navegar pelo principal. O
 * {@code PATTERNS.md} diz que template não decide nada: o controller entrega um
 * objeto pronto para exibir.
 *
 * <p>O motivo concreto: o layout usava
 * {@code sec:authentication="principal.businessName"}, e isso <strong>estoura em
 * toda página</strong> quando o principal não é um {@link AuthenticatedUser} —
 * autenticação anônima, remember-me, ou qualquer caminho de autenticação futuro.
 * Um erro de renderização no layout derruba todas as telas de uma vez.
 *
 * <p><strong>Duas fontes, uma classe só</strong> (pagina-publica-agendamento,
 * TODO-006): sessão autenticada primeiro (área administrativa); se ausente,
 * o atributo de requisição que {@link TenantContextFilter} preenche ao
 * resolver o tenant pelo slug da URL pública. Deliberadamente **não** dois
 * {@code @ControllerAdvice} separados — os dois seriam globais (sem
 * {@code basePackages}/{@code assignableTypes}) e o `@ModelAttribute` de um
 * sobrescreveria o do outro de forma não-determinística, já que a ordem de
 * execução entre advices sem {@code @Order} explícito não é garantida.
 */
@ControllerAdvice
public class LayoutAdvice {

    /** Vazio quando não há sessão nem slug resolvido. */
    @ModelAttribute("businessName")
    public String businessName(Authentication authentication, HttpServletRequest request) {
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser usuario) {
            return usuario.businessName();
        }
        var negocio = request.getAttribute(TenantContextFilter.REQUEST_ATTR_RESOLVED_BUSINESS);
        if (negocio instanceof BusinessRef ref) {
            return ref.name();
        }
        return "";
    }
}
