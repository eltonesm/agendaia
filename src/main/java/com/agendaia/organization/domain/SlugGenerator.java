package com.agendaia.organization.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Deriva o slug do link público a partir do nome do estabelecimento.
 *
 * <p>Java puro, sem estado e sem dependência: é a única lógica com regra de
 * verdade da feature de cadastro, e por isso a mais testada.
 *
 * <p><strong>Esta classe sugere, não decide.</strong> O campo de slug é editável
 * no formulário, então o que vale é o valor submetido — o servidor o valida com
 * {@link #hasValidFormat(String)}, sem rederivar. Ver DD-4 da spec técnica: não
 * existem duas derivações que precisem concordar, existe uma sugestão e uma
 * validação.
 */
public final class SlugGenerator {

    /** Menor slug aceito. Abaixo disso o link fica ambíguo demais. */
    public static final int MIN_LENGTH = 3;

    /** Maior slug aceito, alinhado com a coluna {@code business.slug}. */
    public static final int MAX_LENGTH = 60;

    private static final Pattern DIACRITICOS =
            Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern NAO_ALFANUMERICO = Pattern.compile("[^a-z0-9]+");
    private static final Pattern HIFEN_NAS_PONTAS = Pattern.compile("^-+|-+$");
    private static final Pattern FORMATO_VALIDO =
            Pattern.compile("^[a-z0-9]([a-z0-9-]*[a-z0-9])?$");

    private SlugGenerator() {
        // utilitário
    }

    /**
     * Sugere um slug a partir do nome do estabelecimento.
     *
     * <p>Devolve string vazia quando o nome não produz nenhum caractere
     * aproveitável — por exemplo {@code "!!!"}. Nesse caso o formulário exige
     * que o dono informe o link manualmente, em vez de inventar um.
     */
    public static String from(String nomeDoEstabelecimento) {
        if (nomeDoEstabelecimento == null || nomeDoEstabelecimento.isBlank()) {
            return "";
        }

        // Normalizer separa a letra do acento; a remoção dos diacríticos vem em
        // seguida. Evita tabela de substituição, que sempre esquece um caractere.
        var semAcento = DIACRITICOS
                .matcher(Normalizer.normalize(nomeDoEstabelecimento, Normalizer.Form.NFD))
                .replaceAll("");

        var comHifen = NAO_ALFANUMERICO
                .matcher(semAcento.toLowerCase(Locale.ROOT))
                .replaceAll("-");

        var slug = HIFEN_NAS_PONTAS.matcher(comHifen).replaceAll("");

        if (slug.length() > MAX_LENGTH) {
            slug = HIFEN_NAS_PONTAS.matcher(slug.substring(0, MAX_LENGTH)).replaceAll("");
        }

        return slug;
    }

    /**
     * Confere se o slug submetido pode ser aceito: minúsculas, números e hífen,
     * entre {@value #MIN_LENGTH} e {@value #MAX_LENGTH} caracteres, sem hífen no
     * início ou no fim.
     *
     * <p>Não confere disponibilidade nem palavra reservada — isso é do caso de
     * uso, que precisa do banco.
     */
    public static boolean hasValidFormat(String slug) {
        if (slug == null || slug.length() < MIN_LENGTH || slug.length() > MAX_LENGTH) {
            return false;
        }
        return FORMATO_VALIDO.matcher(slug).matches();
    }

    /**
     * Deriva a n-ésima variação de um slug: {@code barbearia-do-joao} vira
     * {@code barbearia-do-joao-2}.
     *
     * <p>Serve à sugestão que o cadastro oferece quando o link escolhido já
     * está em uso. Puro de propósito: não sabe o que está disponível — quem
     * sabe é o caso de uso, que tem o banco.
     *
     * <p>Se o sufixo estourasse {@value #MAX_LENGTH}, a raiz é encurtada antes,
     * e o hífen que sobrar na ponta é removido: {@code ...-do-} + {@code -2}
     * daria dois hífens seguidos, que o formato recusa.
     */
    public static String variation(String base, int number) {
        var sufixo = "-" + number;
        var limite = MAX_LENGTH - sufixo.length();
        var raiz = base.length() > limite ? base.substring(0, limite) : base;
        raiz = HIFEN_NAS_PONTAS.matcher(raiz).replaceAll("");
        return raiz + sufixo;
    }
}
