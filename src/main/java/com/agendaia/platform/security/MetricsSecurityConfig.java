package com.agendaia.platform.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Login isolado de {@code /actuator/prometheus} — uma ferramenta de coleta
 * automatizada, não uma sessão de navegador (TODO-108, observabilidade,
 * DD-4).
 *
 * <p>Cadeia própria, avaliada antes de {@link OperatorSecurityConfig}
 * ({@code @Order(1)}) e {@link SecurityConfig} ({@code @Order(2)}):
 * {@code securityMatcher("/actuator/prometheus")} garante que nenhuma das
 * três se sobrepõe. {@code /actuator/health} não é tocado por esta cadeia
 * — continua público, resolvido por {@link SecurityConfig} (BR-1 da spec
 * funcional).
 *
 * <p><strong>{@code STATELESS}, diferente das outras duas cadeias</strong>:
 * HTTP Basic reautentica a cada requisição, sem cookie de sessão — não há
 * motivo para persistir {@code SecurityContext} em {@code HttpSession}
 * nem para compartilhar o {@code SecurityContextRepository} das outras
 * cadeias. Consequência: nenhum risco de uma sessão de dono/operador
 * "vazar" acesso a este endpoint, e vice-versa — os mecanismos nem se
 * tocam.
 *
 * <p>Credencial única (BR-2), vinda de configuração — nunca de banco,
 * nunca de formulário. Mesmo padrão de conta única de
 * {@link OperatorSecurityConfig}.
 */
@Configuration
public class MetricsSecurityConfig {

    @Bean
    @Order(0)
    SecurityFilterChain metricsFilterChain(HttpSecurity http, UserDetailsService metricsUserDetailsService)
            throws Exception {
        return http.securityMatcher("/actuator/prometheus")
                .userDetailsService(metricsUserDetailsService)
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("METRICS"))
                .httpBasic(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    /**
     * Conta única (BR-2). {@code password-hash} já vem em BCrypt — nunca em
     * texto puro em configuração, mesma disciplina de
     * {@link OperatorSecurityConfig#operatorUserDetailsService}.
     */
    @Bean
    UserDetailsService metricsUserDetailsService(
            @Value("${agendaia.metrics.username}") String username,
            @Value("${agendaia.metrics.password-hash}") String passwordHash) {
        var metrics =
                User.withUsername(username).password(passwordHash).roles("METRICS").build();
        return new InMemoryUserDetailsManager(metrics);
    }
}
