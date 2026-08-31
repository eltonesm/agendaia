package com.agendaia.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

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

    /**
     * Onde o contexto de segurança é persistido entre requisições.
     *
     * <p>Exposto como bean de propósito: o cadastro precisa autenticar a sessão
     * por conta própria, e tem que gravar **no mesmo lugar** que a cadeia de
     * filtros lê. Deixar cada lado instanciar o seu funcionaria por acidente
     * hoje e quebraria no dia em que um deles mudasse.
     */
    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository contextRepository)
            throws Exception {
        return http.securityContext(sc -> sc.securityContextRepository(contextRepository))
                .authorizeHttpRequests(auth -> auth
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
                .formLogin(form -> form
                        // Nossa tela, agora que ela existe. O GET é servido pelo
                        // view controller do WebConfig; o POST, pelo próprio
                        // Spring Security.
                        .loginPage("/login")
                        // O "false" mantém o destino pretendido: quem foi barrado
                        // ao tentar /admin/agenda volta para lá depois de entrar,
                        // e não para o painel (US-3).
                        .defaultSuccessUrl("/admin/dashboard", false)
                        // Uma mensagem só, para qualquer causa: e-mail
                        // inexistente, senha errada ou conta desativada.
                        // Distinguir revelaria quais e-mails têm conta.
                        .failureUrl("/login?erro")
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
