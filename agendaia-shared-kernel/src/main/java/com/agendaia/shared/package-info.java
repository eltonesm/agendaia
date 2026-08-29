/**
 * Tipos puros compartilhados por todos os contextos delimitados.
 *
 * <p>Sem Spring, sem JPA, sem nada além do JDK — por decisão, não por
 * coincidência. Ver {@code docs/architecture/adr/0001}.
 *
 * <p>Só entra aqui o que é <em>realmente</em> compartilhado e não pertence a
 * nenhum contexto: {@code TenantId}, {@code Money}, {@code TimeRange} e os tipos
 * base de agregado e evento. Se um tipo tem dono natural em algum contexto, ele
 * mora lá — este módulo não é biblioteca de utilidades.
 */
package com.agendaia.shared;
