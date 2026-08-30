package com.agendaia.organization.adapter.out.security;

import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.organization.domain.UserRepository;
import com.agendaia.platform.security.AuthenticatedUser;
import java.util.Locale;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carrega quem está tentando entrar.
 *
 * <p><strong>Mora em {@code organization}, não em {@code platform}</strong>
 * (DD-1). Parece código de segurança, mas precisa da entidade {@code User} — e
 * como todos os contextos dependem de {@code platform}, colocá-lo lá criaria um
 * ciclo que o Spring Modulith reprova. O {@code platform} depende apenas da
 * interface {@link UserDetailsService}, que é biblioteca, não contexto.
 *
 * <p>Duas consultas em vez de uma projeção com join: o login acontece poucas
 * vezes ao dia, e duas consultas simples são mais legíveis do que uma projeção
 * que precisaria de um tipo intermediário atravessando a fronteira de pacote.
 */
@Service
public class BusinessUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;

    public BusinessUserDetailsService(
            UserRepository userRepository, BusinessRepository businessRepository) {
        this.userRepository = userRepository;
        this.businessRepository = businessRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Lança {@link UsernameNotFoundException} quando o e-mail não existe. O
     * {@code DaoAuthenticationProvider} converte isso em credencial inválida
     * antes de chegar ao usuário — e executa uma comparação de senha falsa para
     * que o tempo de resposta não denuncie a diferença entre e-mail inexistente
     * e senha errada.
     *
     * <p><strong>Nenhum log aqui registra o e-mail tentado.</strong> Log vai
     * para arquivo, agregador e backup, e e-mail é dado pessoal.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        var normalizado = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);

        var user = userRepository
                .findByEmail(normalizado)
                .orElseThrow(() -> new UsernameNotFoundException("credencial inválida"));

        var business = businessRepository
                .findById(user.tenantId().value())
                .orElseThrow(() -> new UsernameNotFoundException("credencial inválida"));

        // Usuário inativo e estabelecimento inativo produzem o mesmo efeito:
        // conta desabilitada. O Spring Security devolve a mesma mensagem
        // genérica nos dois casos, sem revelar o motivo.
        var habilitado = user.isActive() && business.isActive();

        return new AuthenticatedUser(
                user.id(),
                user.tenantId(),
                user.email(),
                user.name(),
                business.name(),
                user.passwordHash(),
                user.role().name(),
                habilitado);
    }
}
