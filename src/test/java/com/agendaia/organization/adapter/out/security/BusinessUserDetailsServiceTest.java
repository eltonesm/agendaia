package com.agendaia.organization.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agendaia.organization.application.port.out.BusinessRepository;
import com.agendaia.organization.application.port.out.UserRepository;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.User;
import com.agendaia.platform.security.AuthenticatedUser;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/** Repositórios mockados: nada de banco para verificar regra de autenticação. */
@ExtendWith(MockitoExtension.class)
class BusinessUserDetailsServiceTest {

    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN";

    @Mock private UserRepository userRepository;
    @Mock private BusinessRepository businessRepository;
    @InjectMocks private BusinessUserDetailsService service;

    private Business business() {
        return Business.register("Barbearia do João", "barbearia-do-joao");
    }

    private User userDe(Business business) {
        return User.owner(business.tenantId(), "joao@exemplo.com", "João", HASH);
    }

    @Test
    @DisplayName("monta o principal com tenant e nome do estabelecimento")
    void montaOPrincipal() {
        var business = business();
        var user = userDe(business);
        when(userRepository.findByEmail("joao@exemplo.com")).thenReturn(Optional.of(user));
        when(businessRepository.findById(business.id())).thenReturn(Optional.of(business));

        var detalhes = (AuthenticatedUser) service.loadUserByUsername("joao@exemplo.com");

        assertThat(detalhes.tenantId()).isEqualTo(business.tenantId());
        assertThat(detalhes.businessName()).isEqualTo("Barbearia do João");
        assertThat(detalhes.getUsername()).isEqualTo("joao@exemplo.com");
        assertThat(detalhes.getPassword()).isEqualTo(HASH);
        assertThat(detalhes.isEnabled()).isTrue();
        assertThat(detalhes.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_OWNER");
    }

    @Test
    @DisplayName("normaliza o e-mail antes de consultar")
    void normalizaAntesDeConsultar() {
        var business = business();
        when(userRepository.findByEmail("joao@exemplo.com"))
                .thenReturn(Optional.of(userDe(business)));
        when(businessRepository.findById(business.id())).thenReturn(Optional.of(business));

        service.loadUserByUsername("  Joao@Exemplo.COM  ");

        verify(userRepository).findByEmail("joao@exemplo.com");
    }

    @Test
    @DisplayName("usuário inativo vira conta desabilitada, não exceção distinta")
    void usuarioInativoDesabilita() {
        var business = business();
        var user = userDe(business);
        user.deactivate();
        when(userRepository.findByEmail("joao@exemplo.com")).thenReturn(Optional.of(user));
        when(businessRepository.findById(business.id())).thenReturn(Optional.of(business));

        var detalhes = service.loadUserByUsername("joao@exemplo.com");

        assertThat(detalhes.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("estabelecimento inativo também desabilita — mesmo efeito observável")
    void estabelecimentoInativoDesabilita() {
        var business = business();
        var user = userDe(business);
        business.deactivate();
        when(userRepository.findByEmail("joao@exemplo.com")).thenReturn(Optional.of(user));
        when(businessRepository.findById(business.id())).thenReturn(Optional.of(business));

        var detalhes = service.loadUserByUsername("joao@exemplo.com");

        assertThat(detalhes.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("e-mail inexistente não consulta o estabelecimento e não vaza o motivo")
    void emailInexistente() {
        when(userRepository.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ninguem@exemplo.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("credencial inválida")
                .hasMessageNotContaining("ninguem@exemplo.com");

        verify(businessRepository, never()).findById(any());
    }

    @Test
    @DisplayName("a mensagem nunca contém o e-mail tentado — log e stack são dado pessoal")
    void mensagemNaoContemEmail() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("alvo@exemplo.com"))
                .hasMessageNotContaining("alvo@exemplo.com");
    }
}
