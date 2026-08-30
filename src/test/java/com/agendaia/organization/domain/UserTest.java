package com.agendaia.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.agendaia.shared.TenantId;
import com.agendaia.shared.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final TenantId TENANT = TenantId.of(UuidV7.generate());
    private static final String HASH = "$2a$10$abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMN";

    @Test
    @DisplayName("nasce ativo, como OWNER e com id UUIDv7")
    void nasceAtivo() {
        var user = User.owner(TENANT, "joao@exemplo.com", "João", HASH);

        assertThat(user.isActive()).isTrue();
        assertThat(user.role()).isEqualTo(UserRole.OWNER);
        assertThat(user.id().version()).isEqualTo(7);
        assertThat(user.tenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("normaliza o e-mail — sem isso a unicidade do banco não protegeria")
    void normalizaEmail() {
        var user = User.owner(TENANT, "  Joao@Exemplo.COM  ", "João", HASH);

        assertThat(user.email()).isEqualTo("joao@exemplo.com");
    }

    @Test
    @DisplayName("não existe usuário sem estabelecimento")
    void recusaSemTenant() {
        assertThatThrownBy(() -> User.owner(null, "joao@exemplo.com", "João", HASH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem estabelecimento");
    }

    @Test
    @DisplayName("a senha já precisa chegar como hash — texto claro não entra na entidade")
    void exigeHash() {
        assertThatThrownBy(() -> User.owner(TENANT, "joao@exemplo.com", "João", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hash");
    }

    @Test
    @DisplayName("recusa e-mail vazio")
    void recusaEmailVazio() {
        assertThatThrownBy(() -> User.owner(TENANT, "  ", "João", HASH))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString omite e-mail e hash — e-mail é dado pessoal (LGPD)")
    void toStringNaoVazaDadoPessoal() {
        var user = User.owner(TENANT, "joao@exemplo.com", "João", HASH);

        assertThat(user.toString())
                .doesNotContain("joao@exemplo.com")
                .doesNotContain(HASH)
                .contains(user.id().toString());
    }
}
