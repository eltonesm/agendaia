package com.agendaia.shared;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Gera identificadores UUIDv7, conforme a RFC 9562.
 *
 * <p>Ordenado no tempo: os 48 bits mais significativos são o instante em
 * milissegundos. Isso mantém as inserções na ponta do índice B-tree, em vez de
 * espalhá-las como faria um UUIDv4 aleatório — numa tabela que só cresce, como
 * {@code appointment}, a diferença aparece.
 *
 * <p>Implementado aqui em vez de trazer biblioteca: são poucas linhas de um
 * formato bem especificado, e o Java 21 não oferece nativamente. O
 * {@code uuidv7()} do PostgreSQL 18 existe e funciona, mas gerar no
 * {@code INSERT} acoplaria a identidade do agregado à persistência — o agregado
 * precisa saber quem é no instante em que nasce no domínio (ADR 0009).
 *
 * <p>Layout dos 128 bits:
 *
 * <pre>
 *   0-47    unix_ts_ms   instante em milissegundos
 *   48-51   version      sempre 0111 (7)
 *   52-63   rand_a       12 bits aleatórios
 *   64-65   variant      sempre 10
 *   66-127  rand_b       62 bits aleatórios
 * </pre>
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UuidV7() {
        // utilitário
    }

    public static UUID generate() {
        return generate(System.currentTimeMillis());
    }

    /** Visível para teste: permite fixar o instante e verificar a ordenação. */
    static UUID generate(long unixTimestampMillis) {
        var aleatorio = new byte[10];
        RANDOM.nextBytes(aleatorio);

        // Bits 0-47: timestamp. Bits 48-51: versão 7. Bits 52-63: rand_a.
        long maisSignificativos = (unixTimestampMillis & 0xFFFF_FFFF_FFFFL) << 16;
        maisSignificativos |= 0x7000L; // versão 7
        maisSignificativos |= ((aleatorio[0] & 0x0FL) << 8) | (aleatorio[1] & 0xFFL);

        // Bits 64-65: variante 10. Bits 66-127: rand_b.
        long menosSignificativos = 0;
        for (var i = 2; i < 10; i++) {
            menosSignificativos = (menosSignificativos << 8) | (aleatorio[i] & 0xFFL);
        }
        menosSignificativos &= 0x3FFF_FFFF_FFFF_FFFFL; // zera os dois bits do topo
        menosSignificativos |= 0x8000_0000_0000_0000L; // variante 10

        return new UUID(maisSignificativos, menosSignificativos);
    }
}
