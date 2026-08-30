package com.agendaia.organization.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório do usuário do painel.
 *
 * <p>O e-mail é único no sistema inteiro, não por tenant (BR-1): uma pessoa com
 * dois estabelecimentos precisa de dois e-mails. Por isso as consultas por
 * e-mail não recebem tenant — no login ele ainda nem foi resolvido.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Usado na validação do cadastro, antes de tentar gravar. */
    boolean existsByEmail(String email);

    /** Usado no login. O e-mail chega normalizado em minúsculas. */
    Optional<User> findByEmail(String email);
}
