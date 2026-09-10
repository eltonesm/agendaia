package com.simboraagendar.platform.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Rotas que só renderizam uma tela, sem lógica nenhuma.
 *
 * <p>A tela de login é uma delas: quem processa o POST é o Spring Security, e o
 * GET só precisa devolver o template. Um {@code @Controller} de uma linha para
 * isso seria classe sem conteúdo.
 *
 * <p>A raiz renderiza a landing institucional (pagina-institucional,
 * 2026-09-10) — sem dado de sessão nem de banco, mesmo perfil das demais
 * rotas deste arquivo. Se um dia precisar de dado dinâmico, migra para um
 * {@code @Controller} de verdade.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("auth/login");
        registry.addViewController("/operador/login").setViewName("operador/login");
        registry.addViewController("/").setViewName("landing");
    }
}
