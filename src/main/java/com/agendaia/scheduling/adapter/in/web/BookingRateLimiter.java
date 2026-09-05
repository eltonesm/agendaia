package com.agendaia.scheduling.adapter.in.web;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * BR-8: no máximo 5 tentativas de confirmação por IP a cada 10 minutos.
 *
 * <p>Em memória, sem dependência externa (DD-7 da spec técnica) —
 * reinicia ao reiniciar a aplicação, mesmo trade-off já aceito para
 * sessão (DEBT-013). Vive em {@code scheduling.adapter.in.web}, não
 * {@code platform}: é regra desta feature, não infraestrutura
 * transversal — promoção acontece na terceira ocorrência (PATTERNS.md).
 */
@Component
class BookingRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByIp = new ConcurrentHashMap<>();

    /** @return true se a tentativa é permitida (e já foi registrada); false se o limite foi excedido. */
    boolean tryAcquire(String ip) {
        var agora = Instant.now();
        var tentativas = attemptsByIp.computeIfAbsent(ip, chave -> new ConcurrentLinkedDeque<>());

        synchronized (tentativas) {
            expurgarAntigas(tentativas, agora);
            if (tentativas.size() >= MAX_ATTEMPTS) {
                return false;
            }
            tentativas.addLast(agora);
            return true;
        }
    }

    private static void expurgarAntigas(Deque<Instant> tentativas, Instant agora) {
        var limite = agora.minus(WINDOW);
        while (!tentativas.isEmpty() && tentativas.peekFirst().isBefore(limite)) {
            tentativas.pollFirst();
        }
    }
}
