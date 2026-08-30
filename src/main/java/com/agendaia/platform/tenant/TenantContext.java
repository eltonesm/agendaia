package com.agendaia.platform.tenant;

import com.agendaia.shared.TenantId;
import java.util.Optional;

/**
 * O tenant da requisição em curso.
 *
 * <p>Duas rotas de resolução alimentam este contexto (ADR 0004): a área
 * administrativa resolve pela sessão autenticada; a página pública resolverá
 * pelo slug da URL, quando ela existir. As duas convergem aqui, e daqui sai o
 * filtro que recorta toda consulta.
 *
 * <p><strong>Nunca</strong> preencha isto a partir de parâmetro, corpo ou
 * cabeçalho da requisição. É a fronteira de segurança inteira do produto.
 *
 * <p>Usa {@link ThreadLocal} e não {@code ScopedValue} porque virtual threads
 * estão desligadas de propósito (ver {@code application.yaml}). Se um dia forem
 * ligadas, esta classe é o primeiro lugar a revisar.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantId> ATUAL = new ThreadLocal<>();

    private TenantContext() {
        // utilitário
    }

    public static void set(TenantId tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId não pode ser nulo");
        }
        ATUAL.set(tenantId);
    }

    /** Vazio em rota pública sem slug resolvido, ou antes do filtro rodar. */
    public static Optional<TenantId> current() {
        return Optional.ofNullable(ATUAL.get());
    }

    /**
     * Para código que não pode funcionar sem tenant. Falhar aqui é melhor que
     * consultar sem recorte e devolver dado de outro estabelecimento.
     */
    public static TenantId require() {
        return current()
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhum tenant no contexto. Rota protegida sem filtro de tenant, "
                                + "ou acesso fora do ciclo de uma requisição."));
    }

    /**
     * Obrigatório ao fim de toda requisição, inclusive quando ela falha.
     *
     * <p>Sem isto, o pool de threads devolve a thread com o tenant do
     * requisitante anterior — e a próxima requisição enxerga dado do
     * estabelecimento errado. É a falha mais grave que este projeto pode ter.
     */
    public static void clear() {
        ATUAL.remove();
    }
}
