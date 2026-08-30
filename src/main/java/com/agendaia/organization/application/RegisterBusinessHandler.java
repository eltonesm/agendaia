package com.agendaia.organization.application;

import com.agendaia.organization.application.command.RegisterBusinessCommand;
import com.agendaia.organization.application.port.in.RegisterBusinessUseCase;
import com.agendaia.organization.application.port.in.RegisteredBusiness;
import com.agendaia.organization.domain.Business;
import com.agendaia.organization.domain.BusinessRepository;
import com.agendaia.organization.domain.ReservedSlugs;
import com.agendaia.organization.domain.User;
import com.agendaia.organization.domain.UserRepository;
import com.agendaia.organization.domain.exception.EmailAlreadyUsedException;
import com.agendaia.organization.domain.exception.SlugUnavailableException;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria o estabelecimento e o usuário dono numa única transação.
 *
 * <p>Ou os dois existem, ou nenhum (ADR 0003). É por isso que o cadastro não
 * pode ser dois casos de uso encadeados: um estabelecimento sem dono é uma
 * conta inacessível, e um dono sem estabelecimento não tem onde entrar.
 */
@Service
public class RegisterBusinessHandler implements RegisterBusinessUseCase {

    private static final String CONSTRAINT_SLUG = "business_slug_unique";
    private static final String CONSTRAINT_EMAIL = "app_user_email_unique";

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegisterBusinessHandler(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public RegisteredBusiness register(RegisterBusinessCommand command) {
        var slug = command.slug() == null ? "" : command.slug().strip().toLowerCase(Locale.ROOT);
        var email = command.email() == null ? "" : command.email().strip().toLowerCase(Locale.ROOT);

        // Verificação antecipada: dá erro no campo certo, com mensagem útil.
        // Não é garantia — duas requisições simultâneas passam as duas aqui. A
        // garantia é a restrição do banco, tratada no catch abaixo.
        if (ReservedSlugs.contains(slug) || businessRepository.existsBySlug(slug)) {
            throw new SlugUnavailableException(slug);
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        var business = Business.register(command.businessName(), slug);
        var user = User.owner(
                business.tenantId(),
                email,
                // O nome do usuário começa igual ao do estabelecimento: o
                // formulário pede o mínimo, e este campo vira editável quando
                // houver tela de configuração. Ver Assumptions da spec funcional.
                business.name(),
                passwordEncoder.encode(command.rawPassword()));

        try {
            businessRepository.saveAndFlush(business);
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Chegou aqui: outra requisição gravou entre a verificação e este
            // INSERT. O banco recusou, e agora a corrida vira erro de campo em
            // vez de erro interno — mesma tradução que o ADR 0005 exige para o
            // conflito de horário.
            throw traduzir(e, slug);
        }

        return new RegisteredBusiness(
                business.id(), business.tenantId(), business.name(), business.slug(), user.email());
    }

    /**
     * Converte a violação de restrição na exceção de domínio correspondente.
     *
     * <p>Usa o nome da restrição, e não a mensagem inteira: a mensagem varia
     * entre versões do driver, o nome não.
     */
    private RuntimeException traduzir(DataIntegrityViolationException e, String slug) {
        var causa = e.getMostSpecificCause().getMessage();
        if (causa == null) {
            return e;
        }
        if (causa.contains(CONSTRAINT_SLUG)) {
            return new SlugUnavailableException(slug);
        }
        if (causa.contains(CONSTRAINT_EMAIL)) {
            return new EmailAlreadyUsedException();
        }
        // Violação que não sabemos traduzir é defeito, não regra de negócio:
        // propaga e vira 500 com identificador, para ser investigada.
        return e;
    }
}
