package com.agendaia.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Login isolado do operador da plataforma — quem opera o AgendaIA, não é
 * dono de nenhum estabelecimento (back-office-operador, TODO-009, DD-3).
 *
 * <p>Cadeia própria, avaliada antes de {@link SecurityConfig} (que recua
 * para {@code @Order(2)}): {@code securityMatcher("/operador/**")} garante
 * que as duas nunca se sobrepõem. Credencial única, vinda de configuração
 * — nunca de banco, nunca de formulário (BR-7). O principal é o
 * {@link User} padrão do Spring Security, não {@link AuthenticatedUser}:
 * {@code TenantContextFilter} só reconhece o segundo, então esta sessão
 * nunca ganha {@code tenantId} nenhum, sem precisar mudar aquele filtro.
 *
 * <p><strong>Efeito colateral que exigiu dois ajustes em código já
 * existente</strong>: a partir do momento em que existem dois beans
 * {@code UserDetailsService} no contexto, o Spring Boot para de resolver
 * um sozinho por padrão. {@link SecurityConfig#filterChain} passou a
 * declarar o seu explicitamente ({@code @Qualifier("businessUserDetailsService")}),
 * e {@code RegistrationController} (que também injetava
 * {@code UserDetailsService} por tipo, para autenticar a sessão logo após
 * o cadastro) precisou do mesmo qualifier.
 */
@Configuration
public class OperatorSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain operatorFilterChain(
            HttpSecurity http, SecurityContextRepository contextRepository, UserDetailsService operatorUserDetailsService)
            throws Exception {
        return http.securityMatcher("/operador/**")
                .securityContext(sc -> sc.securityContextRepository(contextRepository))
                .userDetailsService(operatorUserDetailsService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/operador/login", "/css/**", "/js/**", "/img/**", "/favicon.ico")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .formLogin(form -> form
                        .loginPage("/operador/login")
                        .defaultSuccessUrl("/operador/painel", false)
                        .failureUrl("/operador/login?erro")
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/operador/logout")
                        .logoutSuccessUrl("/operador/login?saiu")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.newSession()))
                .build();
    }

    /**
     * Conta única (BR-7). {@code password-hash} já vem em BCrypt — gerado uma
     * vez pelo operador, nunca em texto puro em configuração.
     */
    @Bean
    UserDetailsService operatorUserDetailsService(
            @Value("${agendaia.operador.username}") String username,
            @Value("${agendaia.operador.password-hash}") String passwordHash) {
        var operador =
                User.withUsername(username).password(passwordHash).roles("OPERATOR").build();
        return new InMemoryUserDetailsManager(operador);
    }
}
