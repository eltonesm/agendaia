package com.agendaia.platform.web;

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
 * <p>A raiz leva ao cadastro por ora — é a única porta de entrada que existe.
 * Quando houver página institucional, esta linha muda.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("auth/login");
        registry.addViewController("/operador/login").setViewName("operador/login");
        registry.addRedirectViewController("/", "/cadastro");
    }
}
