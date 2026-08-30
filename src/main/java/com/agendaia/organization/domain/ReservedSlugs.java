package com.agendaia.organization.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Slugs que nenhum estabelecimento pode usar.
 *
 * <p><strong>A lista é curta de propósito.</strong> O ADR 0008 prefixou a rota
 * pública com {@code /b/}, então um slug chamado {@code admin} produz
 * {@code /b/admin}, que não colide com {@code /admin}. A necessidade técnica de
 * uma lista longa desapareceu com aquela decisão.
 *
 * <p>O que resta protege duas coisas: rotas que possam vir a existir sob
 * {@code /b/}, e slugs que confundiriam o cliente final. Crescer esta lista sem
 * um desses dois motivos é cerimônia.
 *
 * <p><strong>Nada com menos de {@value SlugGenerator#MIN_LENGTH} caracteres
 * entra aqui.</strong> O validador de formato já recusa esses, então incluí-los
 * seria peso morto. Foi assim que {@code "b"} e {@code "js"} saíram da lista —
 * o teste {@code reservadasSaoSlugsValidos} os pegou.
 */
public final class ReservedSlugs {

    private static final Set<String> RESERVADOS = Set.of(
            // rotas do sistema, caso o prefixo /b/ mude algum dia
            "admin", "login", "logout", "cadastro", "api", "actuator", "error",
            // recursos estáticos
            "static", "css", "img", "assets", "health",
            // páginas institucionais prováveis
            "sobre", "ajuda", "contato", "termos", "privacidade",
            // subdomínios e valores que denunciam bug
            "app", "www", "null", "undefined");

    private ReservedSlugs() {
        // utilitário
    }

    /** Verdadeiro quando o slug não pode ser usado por um estabelecimento. */
    public static boolean contains(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        return RESERVADOS.contains(slug.toLowerCase(Locale.ROOT));
    }

    /** Cópia imutável, para exibição e teste. */
    public static Set<String> all() {
        return RESERVADOS;
    }
}
