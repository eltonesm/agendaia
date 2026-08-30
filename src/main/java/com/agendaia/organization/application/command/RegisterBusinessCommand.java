package com.agendaia.organization.application.command;

/**
 * Dados do cadastro, já vindos do formulário.
 *
 * <p>A senha chega em texto claro e é a única coisa neste record que não pode
 * aparecer em log. Por isso o {@link #toString()} é sobrescrito: um record
 * padrão imprimiria todos os campos, e este objeto pode acabar numa mensagem de
 * depuração.
 */
public record RegisterBusinessCommand(
        String businessName, String slug, String email, String rawPassword) {

    /** Menor senha aceita. Ver Validation Invariants da spec funcional. */
    public static final int SENHA_MINIMA = 8;

    public RegisterBusinessCommand {
        if (rawPassword == null || rawPassword.length() < SENHA_MINIMA) {
            throw new IllegalArgumentException(
                    "a senha precisa de pelo menos " + SENHA_MINIMA + " caracteres");
        }
    }

    /** Nunca inclui a senha. */
    @Override
    public String toString() {
        return "RegisterBusinessCommand[businessName=%s, slug=%s]".formatted(businessName, slug);
    }
}
