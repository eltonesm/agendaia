package com.agendaia.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.agendaia.shared.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Tradutor de exceção em tela.
 *
 * <p>Escrito depois da revisão de cobertura da TODO-001, que apontou esta como
 * a classe menos coberta do projeto — e é a mesma que já produziu um defeito
 * real nesta feature: o {@code @ExceptionHandler(Exception.class)} engolia as
 * exceções do próprio Spring que já carregam status, e um 404 virava 500.
 *
 * <p>Sem Spring: o handler é uma classe comum, e chamar seus métodos direto é
 * mais rápido e diz mais do que subir contexto para provocar cada exceção.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/x");
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    /** Exceção de negócio concreta — a base é abstrata. */
    private static final class RegraQualquer extends DomainException {
        RegraQualquer() {
            super("Este link já está em uso. Escolha outro.", "slug");
        }
    }

    @Test
    @DisplayName("regra de negócio vira mensagem para o usuário, não identificador de erro")
    void regraDeNegocio() {
        var mav = handler.regraDeNegocioViolada(new RegraQualquer(), request);

        assertThat(mav.getViewName()).isEqualTo("error/500");
        assertThat(mav.getModel().get("mensagem"))
                .isEqualTo("Este link já está em uso. Escolha outro.");
        // Fluxo esperado não ganha identificador: não há o que investigar.
        assertThat(mav.getModel()).doesNotContainKey("requestId");
    }

    @Test
    @DisplayName("página inexistente devolve 404 e a tela de 404, não 500")
    void paginaInexistente() throws Exception {
        // Três argumentos: o Spring 7 acrescentou o caminho do recurso ao lado
        // da URI. O construtor de dois argumentos do Spring 6 não existe mais.
        var naoEncontrada = new NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/nao-existe", "nao-existe");

        var mav = handler.erroInesperado(naoEncontrada, request, response);

        // NoResourceFoundException implementa ErrorResponse mas NÃO estende
        // ErrorResponseException. É exatamente o caso que quebrou antes.
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(mav.getViewName()).isEqualTo("error/404");
        assertThat(mav.getModel()).doesNotContainKey("requestId");
    }

    @Test
    @DisplayName("outro erro do Spring com status próprio preserva o status, e não vira 500")
    void erroDoSpringComStatusProprio() {
        var recusada = new ErrorResponseException(HttpStatus.METHOD_NOT_ALLOWED);

        var mav = handler.erroInesperado(recusada, request, response);

        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(mav.getViewName()).isEqualTo("error/500");
    }

    @Test
    @DisplayName("defeito de verdade vira 500 com identificador rastreável")
    void defeitoDeVerdade() {
        var mav = handler.erroInesperado(new IllegalStateException("boom"), request, response);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(mav.getViewName()).isEqualTo("error/500");

        var requestId = (String) mav.getModel().get("requestId");
        // O mesmo identificador vai para a tela e para o log: sem ele, "deu
        // erro" é irrastreável.
        assertThat(requestId).isNotNull().hasSize(8);
    }

    @Test
    @DisplayName("a tela de defeito nunca mostra a mensagem interna da exceção")
    void naoVazaMensagemInterna() {
        var mav = handler.erroInesperado(
                new IllegalStateException("Sessão aponta para o tenant 018f-... inexistente"),
                request,
                response);

        assertThat(mav.getModel().values()).noneSatisfy(valor -> assertThat(String.valueOf(valor))
                .contains("018f-"));
    }
}
