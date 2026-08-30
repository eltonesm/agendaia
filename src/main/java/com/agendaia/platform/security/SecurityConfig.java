package com.agendaia.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadeia de filtros de segurança.
 *
 * <p>Depende apenas da interface
 * {@link org.springframework.security.core.userdetails.UserDetailsService} —
 * nunca de {@code organization}, que é quem a implementa. É o que evita o ciclo
 * entre módulos descrito no DD-1.
 *
 * <p>Rotas públicas são listadas explicitamente; tudo o mais exige sessão. A
 * ordem inversa — proteger só o que se lembra de proteger — deixa endpoint novo
 * exposto por omissão.
 */
@Configuration
public class SecurityConfig {

    /** Custo padrão. Deliberadamente lento: é a defesa contra força bruta. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/cadastro", "/login", "/error")
                        .permitAll()
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico")
                        .permitAll()
                        // Health é público para sonda de contêiner; os demais
                        // endpoints do actuator expõem informação de operação.
                        .requestMatchers("/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                // Página de login gerada pelo Spring, por enquanto. A TASK-011
                // a substitui por auth/login.html e acrescenta .loginPage("/login").
                //
                // Declarar loginPage ANTES de existir o controller quebraria o
                // fluxo: /login daria 404, e o redirecionamento de quem não está
                // autenticado entraria em laço. Cada commit precisa deixar a
                // aplicação funcionando.
                .formLogin(form -> form
                        // O "false" mantém o destino pretendido: quem foi barrado
                        // ao tentar /admin/agenda volta para lá depois de entrar,
                        // e não para o painel (US-3).
                        .defaultSuccessUrl("/admin/dashboard", false)
                        .permitAll())
                .logout(logout -> logout.logoutUrl("/logout")
                        .logoutSuccessUrl("/login?saiu")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())
                // Renova a sessão na autenticação: sem isto, um identificador de
                // sessão obtido antes do login continuaria válido depois dele.
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.newSession()))
                // CSRF fica ligado (padrão). Os formulários enviam o token pelo
                // th:action do Thymeleaf, sem configuração adicional.
                .build();
    }
}
