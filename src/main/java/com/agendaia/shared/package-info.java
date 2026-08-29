/**
 * Tipos puros compartilhados por todos os contextos delimitados.
 *
 * <p>Sem Spring, sem JPA, sem nada além do JDK. Como o projeto é um único módulo
 * Maven (ADR 0001), o classpath inteiro está tecnicamente disponível aqui — o
 * que torna esta uma regra vigiada por ArchUnit, e não uma impossibilidade
 * física. Ela é levada a sério do mesmo jeito.
 *
 * <p>Só entra aqui o que é <em>realmente</em> compartilhado e não pertence a
 * nenhum contexto: {@code TenantId}, {@code Money}, {@code TimeRange} e os tipos
 * base de agregado e evento. Se um tipo tem dono natural em algum contexto, ele
 * mora lá — este pacote não é biblioteca de utilidades.
 */
package com.agendaia.shared;
