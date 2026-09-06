package com.agendaia.scheduling.adapter.in.web;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.UUID;

/**
 * Gera o corpo de um arquivo {@code .ics} para o agendamento
 * (confirmacao-e-cancelamento, TODO-007, DD-9) — texto plano, sem
 * biblioteca externa: um evento único, sem recorrência, sem fusos
 * múltiplos, não justifica dependência nova.
 *
 * <p>Nunca inclui nome nem telefone do cliente (BR-9, LGPD) — só serviço,
 * horário e nome do estabelecimento.
 */
final class IcsWriter {

    private static final DateTimeFormatter TIMESTAMP_UTC = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4)
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('T')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .appendLiteral('Z')
            .toFormatter(Locale.ROOT);

    private IcsWriter() {
        // utilitário
    }

    static String escrever(String serviceName, String businessName, Instant startsAt, Instant endsAt) {
        return """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//AgendaIA//confirmacao-e-cancelamento//PT
                BEGIN:VEVENT
                UID:%s@agendaia
                DTSTAMP:%s
                DTSTART:%s
                DTEND:%s
                SUMMARY:%s
                LOCATION:%s
                END:VEVENT
                END:VCALENDAR
                """
                .formatted(
                        UUID.randomUUID(),
                        TIMESTAMP_UTC.format(Instant.now().atOffset(ZoneOffset.UTC)),
                        TIMESTAMP_UTC.format(startsAt.atOffset(ZoneOffset.UTC)),
                        TIMESTAMP_UTC.format(endsAt.atOffset(ZoneOffset.UTC)),
                        escapar(serviceName),
                        escapar(businessName));
    }

    /** Escapa vírgula, ponto-e-vírgula e quebra de linha — sintaxe RFC 5545. */
    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace(",", "\\,").replace(";", "\\;").replace("\n", "\\n");
    }
}
