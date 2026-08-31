package com.agendaia.platform.security;

import com.agendaia.shared.TenantId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Quem está autenticado na sessão.
 *
 * <p>Mora em {@code platform}, e não em {@code organization}, para evitar ciclo
 * entre módulos (DD-2): o {@code TenantContextFilter} precisa lê-lo, e
 * {@code platform} é a base da qual todos os contextos dependem. Se o principal
 * vivesse em {@code organization}, {@code platform} passaria a depender de um
 * contexto — e o Spring Modulith reprovaria o build.
 *
 * <p>Quem o constrói é {@code organization}, no login. A seta aponta para dentro
 * nos dois sentidos de uso.
 *
 * <p>Carrega o {@code tenantId} de propósito: é dele que o filtro popula o
 * {@link com.agendaia.platform.tenant.TenantContext}. O tenant nunca vem de
 * parâmetro, corpo ou cabeçalho (ADR 0004).
 *
 * <p>Implementa {@link CredentialsContainer} para que o hash da senha saia do
 * objeto assim que a autenticação termina — achado da revisão de segurança
 * (TASK-016). Sem isso, o principal fica na sessão HTTP <em>carregando o hash
 * BCrypt</em>, e ele acompanharia a sessão para onde ela fosse guardada. Hoje é
 * memória do processo; no dia em que a sessão for para o Redis, o hash iria
 * junto. O {@code passwordHash} é o único campo não-final da classe por causa
 * disso, e é o preço.
 */
public final class AuthenticatedUser implements UserDetails, CredentialsContainer {

    private final UUID userId;
    private final TenantId tenantId;
    private final String email;
    private final String displayName;
    private final String businessName;
    private String passwordHash;
    private final String role;
    private final boolean enabled;

    public AuthenticatedUser(
            UUID userId,
            TenantId tenantId,
            String email,
            String displayName,
            String businessName,
            String passwordHash,
            String role,
            boolean enabled) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.email = email;
        this.displayName = displayName;
        this.businessName = businessName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = enabled;
    }

    public UUID userId() {
        return userId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public String email() {
        return email;
    }

    public String displayName() {
        return displayName;
    }

    /** Exibido no painel; evita uma consulta só para mostrar o nome no cabeçalho. */
    public String businessName() {
        return businessName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    /**
     * Apaga o hash depois que o provider já comparou a senha.
     *
     * <p>Chamado pelo {@code ProviderManager} no login. No cadastro, onde a
     * sessão é autenticada à mão sem passar por provider, quem chama é o
     * controller — o esquecimento ali é justamente o que esta revisão
     * encontrou.
     */
    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Conta desabilitada cobre tanto usuário inativo quanto estabelecimento
     * inativo. O Spring Security produz a mesma mensagem genérica nos dois
     * casos, o que é exatamente o desejado: não revelar o motivo.
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Nunca inclui senha nem e-mail: este objeto pode acabar em log de
     * depuração, e e-mail é dado pessoal.
     */
    @Override
    public String toString() {
        return "AuthenticatedUser[userId=%s, tenantId=%s]".formatted(userId, tenantId);
    }
}
