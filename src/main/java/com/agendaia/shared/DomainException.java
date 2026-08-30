package com.agendaia.shared;

/**
 * Base de toda exceção que representa regra de negócio violada.
 *
 * <p>Mora em {@code shared} — Java puro — e não em {@code platform}, por uma
 * razão de dependência: exceções de domínio vivem em
 * {@code <contexto>.domain}, e o domínio de {@code scheduling} não pode importar
 * nada com Spring dentro. Se a base estivesse em {@code platform}, o domínio
 * passaria a depender de framework por tabela.
 *
 * <p>Quem traduz isto em tela é o {@code GlobalExceptionHandler} do
 * {@code platform}, que pode depender de {@code shared} sem problema.
 *
 * <p>Exceção de negócio é <strong>fluxo esperado</strong>: vira log em
 * {@code WARN} sem stack trace, e mensagem para o usuário. Nunca 500.
 */
public abstract class DomainException extends RuntimeException {

    /**
     * Campo do formulário que originou o problema, quando houver.
     *
     * <p>Permite ao adapter web devolver o erro no campo certo em vez de uma
     * mensagem solta no topo da página — que é o que preserva o preenchimento
     * e evita que o usuário reescreva tudo.
     */
    private final String field;

    protected DomainException(String message) {
        this(message, null);
    }

    protected DomainException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }

    public boolean hasField() {
        return field != null && !field.isBlank();
    }

    /**
     * Exceção de negócio não precisa de stack trace: ela descreve uma situação
     * prevista, não um defeito. Suprimir economiza custo e evita poluir o log
     * com rastro que ninguém vai ler.
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
