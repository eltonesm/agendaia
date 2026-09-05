package com.agendaia.organization.adapter.in.web;

import com.agendaia.organization.adapter.in.web.request.RegistrationRequest;
import com.agendaia.organization.application.command.RegisterBusinessCommand;
import com.agendaia.organization.application.port.in.RegisterBusinessUseCase;
import com.agendaia.shared.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Cadastro do estabelecimento.
 *
 * <p>Não conhece repositório nem entidade: fala com o caso de uso e devolve
 * tela. O que ele acrescenta ao fluxo é a autenticação da sessão — que é
 * responsabilidade da camada web, não do caso de uso.
 */
@Controller
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private static final String VIEW = "auth/cadastro";
    private static final String PAINEL = "redirect:/admin/dashboard";

    private final RegisterBusinessUseCase registerBusiness;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;

    public RegistrationController(
            RegisterBusinessUseCase registerBusiness,
            @Qualifier("businessUserDetailsService") UserDetailsService userDetailsService,
            SecurityContextRepository securityContextRepository) {
        this.registerBusiness = registerBusiness;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/cadastro")
    public String formulario(Model model) {
        model.addAttribute("form", new RegistrationRequest());
        return VIEW;
    }

    @PostMapping("/cadastro")
    public String cadastrar(
            @Valid @ModelAttribute("form") RegistrationRequest form,
            BindingResult binding,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Erro de formato devolve a MESMA tela com 200, não 400: é formulário
        // HTML, não API. O objeto no model preserva o que já foi digitado.
        if (binding.hasErrors()) {
            return VIEW;
        }

        try {
            var registrado = registerBusiness.register(new RegisterBusinessCommand(
                    form.businessName(), form.slug(), form.email(), form.password()));

            autenticarSessao(registrado.ownerEmail(), request, response);

            log.info(
                    "Estabelecimento cadastrado: businessId={} slug={}",
                    registrado.businessId(),
                    registrado.slug());

            return PAINEL;

        } catch (DomainException e) {
            // Erro de negócio vira erro NO CAMPO que o causou, preservando o
            // resto do preenchimento. Mensagem solta no topo faria o dono
            // reescrever tudo.
            if (e.hasField()) {
                binding.rejectValue(e.field(), "indisponivel", e.getMessage());
            } else {
                binding.reject("erro", e.getMessage());
            }
            return VIEW;
        }
    }

    /**
     * Autentica a sessão logo após o cadastro, sem pedir a senha de novo.
     *
     * <p><strong>Grava no {@link SecurityContextRepository}, não apenas no
     * {@link SecurityContextHolder}</strong> — é o DD-5, e é o defeito mais
     * provável desta feature. A partir do Spring Security 6 o
     * {@code SecurityContextHolder} vive na thread da requisição e <em>não</em>
     * chega à sessão: o usuário sairia autenticado deste POST e chegaria
     * deslogado no redirecionamento para o painel, com a suíte de testes verde.
     *
     * <p>Carrega o principal pelo mesmo {@code UserDetailsService} do login, em
     * vez de montá-lo aqui: garante que a sessão criada no cadastro seja
     * idêntica à criada ao entrar.
     */
    private void autenticarSessao(
            String email, HttpServletRequest request, HttpServletResponse response) {

        var principal = userDetailsService.loadUserByUsername(email);

        // Apaga o hash antes de o principal entrar na sessão. No login isso é o
        // ProviderManager quem faz; aqui não há provider nenhum, então é
        // responsabilidade deste método. Achado da revisão de segurança
        // (TASK-016).
        if (principal instanceof CredentialsContainer credenciais) {
            credenciais.eraseCredentials();
        }

        var autenticacao = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);

        // Esta linha é a que faz a autenticação sobreviver ao redirecionamento.
        securityContextRepository.saveContext(contexto, request, response);
    }
}
