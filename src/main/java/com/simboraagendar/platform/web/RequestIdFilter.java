package com.simboraagendar.platform.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gera um identificador de correlação por requisição HTTP (TODO-108,
 * observabilidade) — presente em toda linha de log da requisição, e no
 * cabeçalho de resposta {@code X-Request-Id}.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)}: precisa rodar antes de tudo,
 * inclusive do Spring Security — diferente de {@code TenantContextFilter}
 * (que precisa do principal já resolvido), este não depende de nada e
 * toda requisição merece um id, resolvendo tenant ou não, autenticando
 * ou não.
 *
 * <p>Mesmo formato truncado (8 caracteres hex) que
 * {@code GlobalExceptionHandler} já usava isoladamente para a tela de
 * erro 500 — esta feature unifica as duas fontes num id só, lido do MDC.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    static final String MDC_REQUEST_ID = "requestId";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        var requestId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(HEADER_REQUEST_ID, requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            // Mesma disciplina de TenantContextFilter: sem isto, a próxima
            // requisição na mesma thread do pool herdaria este id.
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
