package com.simboraagendar.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Sem Spring context — o filtro é uma classe comum, chamado direto. */
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void limparMdc() {
        MDC.remove(RequestIdFilter.MDC_REQUEST_ID);
    }

    @Test
    @DisplayName("popula requestId no MDC durante a cadeia, com o mesmo valor do cabeçalho de resposta")
    void populaMdcEHeaderDuranteACadeia() throws Exception {
        var request = new MockHttpServletRequest("GET", "/qualquer");
        var response = new MockHttpServletResponse();
        var idDuranteACadeia = new String[1];
        var chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                // Capturado DURANTE a cadeia — é aqui que o resto da
                // aplicação (log, controller) roda de verdade. Depois que
                // doFilter devolve, o finally já limpou o MDC (próximo teste).
                idDuranteACadeia[0] = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
            }
        };

        filter.doFilter(request, response, chain);

        assertThat(idDuranteACadeia[0]).isNotNull();
        assertThat(response.getHeader("X-Request-Id")).isEqualTo(idDuranteACadeia[0]);
    }

    @Test
    @DisplayName("limpa o MDC depois da cadeia — nunca vaza para a próxima requisição na mesma thread")
    void limpaMdcDepoisDaCadeia() throws Exception {
        var request = new MockHttpServletRequest("GET", "/qualquer");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("cada requisição ganha um requestId diferente")
    void cadaRequisicaoGanhaIdDiferente() throws Exception {
        var idsCapturados = new String[2];
        var chain1 = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                idsCapturados[0] = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
            }
        };
        var chain2 = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                idsCapturados[1] = MDC.get(RequestIdFilter.MDC_REQUEST_ID);
            }
        };

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain1);
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain2);

        assertThat(idsCapturados[0]).isNotNull().isNotEqualTo(idsCapturados[1]);
    }

    @Test
    @DisplayName("MDC é limpo mesmo quando a cadeia lança exceção")
    void limpaMdcMesmoComExcecaoNaCadeia() {
        var chainQueFalha = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                throw new IllegalStateException("falha simulada da cadeia");
            }
        };

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> filter.doFilter(
                        new MockHttpServletRequest(), new MockHttpServletResponse(), chainQueFalha))
                .isInstanceOf(IllegalStateException.class);

        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID)).isNull();
    }
}
