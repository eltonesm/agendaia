package com.agendaia;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasName;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Regras de arquitetura <strong>dentro</strong> de cada contexto.
 *
 * <p>A fronteira <strong>entre</strong> contextos é responsabilidade do Spring
 * Modulith, em {@link ModuleStructureTest} — os dois escopos não se sobrepõem,
 * conforme o ADR 0010.
 *
 * <p>Como o projeto é um único módulo Maven (ADR 0001), nada aqui é garantido
 * pelo compilador. Estas regras <em>são</em> a arquitetura: sem elas, o que está
 * escrito nos documentos é recomendação.
 *
 * <p>Os testes analisam também as classes de teste, de propósito: a regra de
 * pureza do domínio precisa valer para o teste de domínio também, senão um
 * {@code @SpringBootTest} para verificar um value object passa despercebido.
 */
@AnalyzeClasses(packages = "com.agendaia")
class ArchitectureTest {

    private static final String[] FRAMEWORKS_PROIBIDOS_NO_NUCLEO = {
        "org.springframework..", "jakarta.persistence..", "jakarta.servlet..", "org.hibernate.."
    };

    @ArchTest
    static final ArchRule o_dominio_do_nucleo_nao_conhece_framework = noClasses()
            .that()
            .resideInAPackage("..scheduling.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(FRAMEWORKS_PROIBIDOS_NO_NUCLEO)
            .because("""
                    scheduling é o core domain e roda no regime completo de Clean \
                    Architecture (ADR 0002): o domínio é Java puro, testável em \
                    milissegundos sem subir Spring nem banco. Contextos de suporte \
                    não têm essa restrição — neles a entidade JPA É o modelo.""");

    @ArchTest
    static final ArchRule application_nao_conhece_adapter = noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..adapter..")
            .because("""
                    a regra de dependência aponta para dentro: o caso de uso define \
                    a porta, o adapter a implementa. O contrário inverte a seta.""");

    /**
     * Repositório <strong>nosso</strong>, não qualquer classe cujo nome termine
     * em Repository.
     *
     * <p>A primeira versão desta regra não tinha o recorte de pacote e acusou
     * três violações no {@code RegistrationController} por causa do
     * {@code SecurityContextRepository} — que é armazenamento de sessão do
     * Spring Security, e não repositório de dados. Sessão é responsabilidade
     * legítima da camada web.
     *
     * <p>Regra com falso positivo é pior que regra ausente: ela treina o time a
     * enfraquecê-la ou apagá-la.
     */
    private static final DescribedPredicate<JavaClass> REPOSITORIO_DO_PROJETO =
            JavaClass.Predicates.resideInAPackage("com.agendaia..")
                    .and(HasName.Predicates.nameMatching(".*Repository"))
                    .as("repositório do projeto");

    @ArchTest
    static final ArchRule controller_nao_fala_com_repositorio = noClasses()
            .that()
            .haveSimpleNameEndingWith("Controller")
            .should()
            .dependOnClassesThat(REPOSITORIO_DO_PROJETO)
            .because("""
                    o fluxo é Controller → UseCase → Domain → Port → Adapter. \
                    Controller que injeta repositório pula o caso de uso e leva \
                    regra de negócio para a camada web.""");

    @ArchTest
    static final ArchRule transacao_so_na_application = noClasses()
            .that()
            .resideOutsideOfPackage("..application..")
            .should()
            .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .orShould()
            .beAnnotatedWith("jakarta.transaction.Transactional")
            .because("""
                    o domínio não sabe que existe banco e o controller não decide \
                    fronteira de consistência. Transação é decisão de caso de uso.""");

    /**
     * A regra acima olha só a classe. Um {@code @Transactional} em método
     * passava livre — foi assim que o {@code BusinessUserDetailsService} manteve
     * uma transação em {@code adapter} até a revisão da TASK-014. Regra que
     * cobre metade do que promete é pior que regra ausente.
     */
    @ArchTest
    static final ArchRule transacao_em_metodo_so_na_application = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideOutsideOfPackage("..application..")
            .should()
            .beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .orShould()
            .beAnnotatedWith("jakarta.transaction.Transactional")
            .because("""
                    anotar o método em vez da classe não muda de quem é a decisão: \
                    a fronteira de consistência continua sendo do caso de uso.""");

    @ArchTest
    static final ArchRule sem_setter_publico_no_dominio = noMethods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAnyPackage("..domain..", "..api..")
            .and()
            .arePublic()
            .should()
            .haveNameMatching("set[A-Z].*")
            .because("""
                    setter público devolve ao chamador a responsabilidade de manter \
                    a invariante, e o objeto deixa de garantir qualquer coisa sobre \
                    si. Estado muda por método de negócio: confirm(), cancel(reason).""");

    @ArchTest
    static final ArchRule caso_de_uso_e_interface = classes()
            .that()
            .haveSimpleNameEndingWith("UseCase")
            .should()
            .beInterfaces()
            .because("""
                    convenção do time: comunicação entre camadas passa por interface. \
                    A implementação usa o sufixo Handler — nunca Impl, que é sintoma \
                    de que não existe um segundo conceito.""");

    @ArchTest
    static final ArchRule implementacao_nao_usa_sufixo_impl = noClasses()
            .should()
            .haveSimpleNameEndingWith("Impl")
            .because("""
                    XImpl significa que ninguém achou um segundo nome. Use o nome do \
                    papel: Handler para caso de uso, PersistenceAdapter para saída.""");

    @ArchTest
    static final ArchRule entidade_jpa_so_no_adapter_do_nucleo = noClasses()
            .that()
            .resideInAPackage("..scheduling..")
            .and()
            .areAnnotatedWith("jakarta.persistence.Entity")
            .should()
            .resideOutsideOfPackage("..adapter.out.persistence..")
            .because("""
                    em scheduling, a entidade JPA é classe separada do modelo de \
                    domínio e vive no adapter de persistência, com mapeamento \
                    explícito entre as duas (ADR 0002).""");
}
