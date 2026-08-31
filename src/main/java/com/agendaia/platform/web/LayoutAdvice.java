package com.agendaia.platform.web;

import com.agendaia.platform.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Coloca no model o que toda tela autenticada exibe.
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
 */
@ControllerAdvice
public class LayoutAdvice {

    /** Vazio quando não há sessão ou o principal é de outro tipo. */
    @ModelAttribute("businessName")
    public String businessName(Authentication authentication) {
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser usuario) {
            return usuario.businessName();
        }
        return "";
    }
}
