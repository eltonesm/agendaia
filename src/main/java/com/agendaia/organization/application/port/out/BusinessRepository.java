package com.agendaia.organization.application.port.out;

import com.agendaia.organization.domain.Business;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Porta de saída do estabelecimento — interface do Spring Data direto, sem
 * adapter próprio: em subdomínio de suporte a entidade JPA é o modelo, e
 * separar interface de implementação aqui seria a cerimônia que o ADR 0002
 * dispensa. O que muda de {@code domain} para {@code application.port.out} é
 * só o pacote: repositório é a forma do caso de uso pedir persistência, não
 * parte do modelo — domínio não conhece a existência de banco, nem como
 * interface.
 *
 * <p>Sem recorte por tenant nas consultas: {@code business} <em>é</em> a tabela
 * de tenants. É a única exceção do projeto.
 */
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    /** Usado na validação do cadastro, antes de tentar gravar. */
    boolean existsBySlug(String slug);

    /** Resolução do tenant pela URL pública, a partir da TODO-006. */
    Optional<Business> findBySlug(String slug);

    /** Usado por {@code BusinessDirectory.listAll()} (back-office-operador, DD-5) — sem filtro de tenant, de propósito. */
    List<Business> findAllByOrderByCreatedAtAsc();
}
