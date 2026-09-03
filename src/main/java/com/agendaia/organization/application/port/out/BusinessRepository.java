package com.agendaia.organization.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório do estabelecimento.
 *
 * <p>Interface do Spring Data direto no domínio: em subdomínio de suporte a
 * entidade JPA é o modelo, e criar uma interface própria para depois
 * implementá-la com Spring Data seria a cerimônia que o ADR 0002 dispensa.
 *
 * <p>Sem recorte por tenant nas consultas: {@code business} <em>é</em> a tabela
 * de tenants. É a única exceção do projeto.
 */
public interface BusinessRepository extends JpaRepository<Business, UUID> {

    /** Usado na validação do cadastro, antes de tentar gravar. */
    boolean existsBySlug(String slug);

    /** Resolução do tenant pela URL pública, a partir da TODO-006. */
    Optional<Business> findBySlug(String slug);
}
