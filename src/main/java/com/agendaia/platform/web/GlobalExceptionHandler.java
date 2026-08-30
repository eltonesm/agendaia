package com.agendaia.platform.web;

import com.agendaia.shared.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.web.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * Traduz exceção em tela.
 *
 * <p>Dois tratamentos, deliberadamente diferentes:
 *
 * <ul>
 *   <li><strong>{@link DomainException}</strong> — fluxo esperado. Log em
 *       {@code WARN} sem stack trace, mensagem em português para o usuário.
 *       Nunca 500: conflito de horário ou slug em uso não é defeito.
 *   <li><strong>Qualquer outra</strong> — defeito. Log em {@code ERROR} com
 *       stack trace e um identificador, e a tela mostra o mesmo identificador.
 *       É o que liga a reclamação do usuário à linha certa do log.
 * </ul>
 *
 * <p>Este é o tratamento de <em>última instância</em>. Erro de formulário é
 * tratado pelo próprio controller, que devolve a mesma tela com o erro no campo
 * — preservando o preenchimento. Chegar aqui com {@code DomainException} de
 * formulário significa que o controller deixou escapar.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // UNPROCESSABLE_CONTENT, não UNPROCESSABLE_ENTITY: a RFC 9110 renomeou o
    // 422, e o Spring 7 depreciou o nome antigo.
    @ExceptionHandler(DomainException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ModelAndView regraDeNegocioViolada(DomainException e, HttpServletRequest request) {
        // Sem stack trace: a situação é prevista, não um defeito. Poluir o log
        // com rastro de fluxo esperado esconde o defeito real.
        log.warn("Regra de negócio violada em {}: {}", request.getRequestURI(), e.getMessage());

        var mav = new ModelAndView("error/500");
        mav.addObject("mensagem", e.getMessage());
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView erroInesperado(
            Exception e, HttpServletRequest request, HttpServletResponse response) {

        // Exceção do próprio Spring que JÁ carrega o status certo: página
        // inexistente, método não suportado, mídia não aceita. Todas implementam
        // ErrorResponse — mas nem todas estendem ErrorResponseException, então a
        // checagem tem que ser pela interface. NoResourceFoundException é
        // justamente uma das que não estende.
        //
        // Sem este desvio, um 404 vira 500 e o usuário lê "algo deu errado"
        // quando apenas digitou um endereço que não existe. Foi o SecurityRoutesIT
        // que pegou.
        if (e instanceof ErrorResponse resposta) {
            var status = resposta.getStatusCode().value();
            response.setStatus(status);
            // 4xx é uso incorreto, não defeito: WARN e sem stack trace.
            log.warn("Requisição rejeitada com {} em {}", status, request.getRequestURI());
            return new ModelAndView(status == 404 ? "error/404" : "error/500");
        }

        var requestId = UUID.randomUUID().toString().substring(0, 8);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        // O identificador vai para o log E para a tela. Sem ele, "deu erro"
        // é irrastreável.
        log.error("Erro inesperado [{}] em {}", requestId, request.getRequestURI(), e);

        var mav = new ModelAndView("error/500");
        mav.addObject("requestId", requestId);
        return mav;
    }
}
