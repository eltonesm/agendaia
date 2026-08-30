# cadastro-estabelecimento-login - Functional Spec

**Feature**: cadastro-estabelecimento-login
**Backlog**: TODO-001
**Status**: draft
**Idioma**: português (cabeçalhos de seção em inglês, por contrato do template)
**Data**: 2026-08-30

---

## Problem Statement

O AgendaIA não tem nenhuma porta de entrada. Não existe forma de um
estabelecimento passar a existir no sistema, e sem estabelecimento não existe
tenant — logo nada mais tem onde morar: nem profissional, nem serviço, nem
agendamento.

Esta feature abre a porta e estabelece a fronteira de isolamento sobre a qual
todas as outras se apoiam.

---

## Objectives

1. Um estabelecimento cria a própria conta sozinho, sem intervenção manual.
2. O tenant passa a existir e a isolar todos os dados criados depois.
3. O dono autentica e o `TenantContext` é populado a partir da sessão.
4. O estabelecimento sai do cadastro sabendo qual é o seu link público.

---

## Scope

### In Scope

- Formulário de cadastro com três campos: nome do estabelecimento, e-mail e senha.
- Slug derivado automaticamente do nome, visível e editável antes de confirmar.
- Validação do slug: formato, unicidade global e lista de palavras reservadas.
- Criação de `Business` e `User` numa única transação.
- Autenticação por e-mail e senha, com sessão.
- Login automático logo após o cadastro, sem pedir a senha de novo.
- Painel mínimo autenticado, exibindo o nome do estabelecimento e o link público.
- Encerramento de sessão.

### Out of Scope

- Recuperação de senha — registrada como TODO-109 no backlog.
- Confirmação de e-mail: exigiria infraestrutura de e-mail transacional, que o
  projeto não tem, e puxaria SMTP para dentro desta feature.
- Edição dos dados do estabelecimento depois de criado.
- Cadastro de profissional — é a TODO-002.
- A página pública `/b/{slug}` em si — é a TODO-006. Aqui o link apenas existe
  e é exibido.
- Múltiplos usuários por estabelecimento, papéis e permissões — gatilho
  registrado na IDEA-011.
- Cadastro de estabelecimento por administrador da plataforma.

---

## User Stories

### US-1: Criar a conta do estabelecimento

**As a** dono de barbearia que hoje anota os horários num caderno
**I want** criar minha conta informando o nome do estabelecimento, meu e-mail e uma senha
**So that** eu passe a ter um lugar meu para organizar a agenda
**Priority**: High

**Acceptance Criteria**:

- [ ] O formulário pede exatamente três campos: nome do estabelecimento, e-mail e senha.
- [ ] O nome do estabelecimento tem entre 2 e 120 caracteres.
- [ ] O e-mail é validado quanto ao formato e precisa ser único no sistema inteiro.
- [ ] A senha tem no mínimo 8 caracteres e é armazenada com BCrypt, nunca em texto claro.
- [ ] Ao confirmar, o estabelecimento e o usuário dono são criados na mesma transação: ou os dois existem, ou nenhum.
- [ ] Se o e-mail já estiver em uso, a mensagem indica o campo e preserva o que já foi digitado.

### US-2: Escolher o endereço do link público

**As a** dono do estabelecimento
**I want** ver e ajustar o endereço do meu link antes de confirmar o cadastro
**So that** eu compartilhe com meus clientes um link que representa meu negócio
**Priority**: High

**Acceptance Criteria**:

- [ ] O slug é derivado do nome enquanto ele é digitado: "Barbearia do João" resulta em `barbearia-do-joao`.
- [ ] A derivação remove acentos, converte para minúsculas e troca espaços e pontuação por hífen.
- [ ] O campo de slug é editável antes de confirmar.
- [ ] O endereço completo é exibido ao lado do campo, como `agendaia.com/b/barbearia-do-joao`.
- [ ] O slug é único no sistema inteiro; se já estiver em uso, o cadastro é recusado com mensagem no campo.
- [ ] Um slug que conste da lista de palavras reservadas é recusado.
- [ ] O formato aceito é: minúsculas, números e hífen, entre 3 e 60 caracteres, sem hífen no início ou no fim.

### US-3: Entrar na conta

**As a** dono do estabelecimento já cadastrado
**I want** entrar com meu e-mail e senha
**So that** eu acesse a agenda e as configurações do meu negócio
**Priority**: High

**Acceptance Criteria**:

- [ ] O login aceita e-mail e senha.
- [ ] Credencial incorreta produz uma única mensagem genérica, sem revelar se o e-mail existe.
- [ ] Sessão autenticada popula o `TenantContext` com o tenant do usuário.
- [ ] Usuário ou estabelecimento inativo não consegue entrar.
- [ ] Acesso a qualquer rota sob `/admin/**` sem sessão redireciona para o login.
- [ ] Depois de entrar pelo redirecionamento, o usuário volta para a rota que tentou acessar.

### US-4: Sair da conta

**As a** dono do estabelecimento
**I want** encerrar minha sessão
**So that** ninguém use minha conta em um computador compartilhado
**Priority**: Medium

**Acceptance Criteria**:

- [ ] Existe uma ação de sair visível em toda tela autenticada.
- [ ] Ao sair, a sessão é invalidada no servidor.
- [ ] Voltar pelo histórico do navegador não devolve acesso a rota autenticada.

---

## Business Rules

### Core Rules

- **BR-1**: O e-mail é único no sistema inteiro. Uma pessoa com dois estabelecimentos precisa de dois e-mails.
- **BR-2**: O slug é único no sistema inteiro e imutável nesta feature — alteração é evolução futura, já prevista pelo ADR 0011.
- **BR-3**: `Business` e `User` nascem na mesma transação. Não existe estabelecimento sem dono, nem dono sem estabelecimento.
- **BR-4**: O `Business` recém-criado é o tenant. Seu identificador é o `tenant_id` de todos os dados subsequentes.
- **BR-5**: A senha é armazenada com BCrypt. O texto claro não é registrado em log, nem em mensagem de erro, nem em campo do banco.
- **BR-6**: Ambos nascem ativos (`active = true`).

### Calculations

**Derivação do slug** a partir do nome do estabelecimento:

| Entrada | Saída |
|---|---|
| `Barbearia do João` | `barbearia-do-joao` |
| `Salão & Cia.` | `salao-cia` |
| `Studio  da   Ana` | `studio-da-ana` |
| `Corte 10` | `corte-10` |
| `--Barbearia--` | `barbearia` |

Regras: remover acentuação, converter para minúsculas, substituir qualquer
sequência de caracteres não alfanuméricos por um hífen único, remover hífen do
início e do fim.

### Validation Invariants

- Nome do estabelecimento: 2 a 120 caracteres, não vazio depois de remover espaços das pontas.
- E-mail: formato válido, no máximo 254 caracteres, normalizado para minúsculas.
- Senha: mínimo 8 caracteres. Sem exigência de símbolo ou maiúscula — regra que aumenta abandono sem aumentar segurança de forma proporcional.
- Slug: `^[a-z0-9]([a-z0-9-]*[a-z0-9])?$`, de 3 a 60 caracteres.

### Exceptions

**Palavras reservadas para slug**:

`admin`, `login`, `logout`, `cadastro`, `api`, `actuator`, `error`, `b`,
`static`, `css`, `js`, `img`, `assets`, `health`, `sobre`, `ajuda`, `contato`,
`termos`, `privacidade`, `app`, `www`, `null`, `undefined`.

> **Nota sobre o tamanho desta lista**: o ADR 0008 já prefixa a rota pública com
> `/b/`, então um slug chamado `admin` gera `/b/admin`, que **não** colide com
> `/admin`. A necessidade técnica da lista praticamente desapareceu com aquela
> decisão. O que resta é proteger rotas futuras sob `/b/` e evitar slugs
> confusos. A lista é curta de propósito; crescê-la sem motivo é cerimônia.

---

## Data Model

### Input Data

| Campo | Origem | Obrigatório | Observação |
|---|---|---|---|
| Nome do estabelecimento | formulário | sim | também alimenta a derivação do slug |
| E-mail | formulário | sim | vira credencial de login |
| Senha | formulário | sim | nunca persistida em claro |
| Slug | derivado, editável | sim | preenchido automaticamente, ajustável |

### Output Data

Estabelecimento criado, com identificador próprio, nome, slug, fuso horário e
situação ativa. Usuário dono criado, com e-mail, nome, senha protegida, papel de
proprietário e situação ativa. Sessão autenticada com o tenant resolvido.

### Data Transformations

- Nome → slug, conforme a tabela em Calculations.
- E-mail → minúsculas, espaços das pontas removidos.
- Senha → hash BCrypt.
- Nome do estabelecimento → nome inicial do usuário, por não ser pedido no formulário.

---

## User Experience

### User Personas

**João, 34 anos, dono de barbearia com uma cadeira.** Usa o celular para quase
tudo e o computador raramente. Nunca usou sistema de gestão. Desiste de
formulário longo. Vai criar a conta uma única vez na vida, provavelmente à
noite, depois de fechar.

### User Journey Map

```
Abre /cadastro
      ↓
Digita o nome do estabelecimento
      ↓
Vê o link aparecer sozinho: agendaia.com/b/barbearia-do-joao
      ↓
Ajusta o link, se quiser
      ↓
Digita e-mail e senha
      ↓
Confirma
      ↓
Já está dentro do painel — sem passar pela tela de login
      ↓
Vê o nome do estabelecimento e o link público
```

### Alternative Flows

- **Já tem conta**: a tela de cadastro leva ao login, e a de login leva ao cadastro.
- **Tentou abrir rota autenticada sem sessão**: vai para o login e, depois de entrar, volta para onde tentou ir.

### Edge Cases & Error Handling

| Situação | Comportamento |
|---|---|
| E-mail já cadastrado | Erro no campo do e-mail; os demais campos preservam o que foi digitado |
| Slug já em uso | Erro no campo do slug, sugerindo uma variação disponível |
| Slug é palavra reservada | Erro no campo do slug, com a mesma mensagem de indisponível |
| Nome só com pontuação, gerando slug vazio | Erro pedindo que o link seja informado manualmente |
| Senha com menos de 8 caracteres | Erro no campo da senha |
| Credencial de login incorreta | Mensagem única e genérica, sem distinguir e-mail inexistente de senha errada |
| Conta ou estabelecimento inativo | Mesma mensagem genérica do login incorreto |
| Duas pessoas confirmam o mesmo slug ao mesmo tempo | O banco recusa a segunda; a tela mostra erro de indisponibilidade, não erro interno |

### UI/UX References

Sem wireframe. Três telas, montadas com Bootstrap 5 conforme o ADR 0012:
`/cadastro`, `/login` e `/admin/dashboard`. O painel desta feature é
deliberadamente mínimo — exibe o nome, o link público e o que fazer em seguida.

---

## Critical E2E Test Scenarios

> O `meta.md` registra `ltp_enabled: false` — o framework de E2E é interno do
> Mercado Livre e não existe aqui. Estes cenários **não** viram arquivos naquele
> framework; são o contrato do que precisa passar antes de a feature ser
> considerada pronta, e serão implementados com `MockMvc` e Testcontainers.

### E2E-1: Cadastro completo até o painel (caminho feliz)

**Criticidade**: 🔴 Critical — sem este caminho a feature nao existe.

1. Abrir `/cadastro`.
2. Digitar "Barbearia do João" no nome.
3. Conferir que o campo de link foi preenchido sozinho com `barbearia-do-joao`.
4. Digitar e-mail e senha válidos.
5. Confirmar.

**Resultado esperado**: redirecionado para `/admin/dashboard` **já
autenticado**, sem passar pela tela de login. O painel exibe "Barbearia do João"
e o link público. No banco existem um `Business` e um `User`, ambos ativos, com
o mesmo `tenant_id`.

### E2E-2: Entrar em conta existente (fluxo alternativo)

**Criticidade**: 🟡 Important — o redirecionamento de volta e o que costuma quebrar.

1. Com um estabelecimento já cadastrado, abrir `/admin/agenda` sem sessão.
2. Ser redirecionado para `/login`.
3. Informar e-mail e senha corretos.

**Resultado esperado**: autenticado e redirecionado de volta para
`/admin/agenda` — a rota originalmente pretendida, não o painel. O
`TenantContext` contém o tenant do usuário.

### E2E-3: Slug já em uso (tratamento de erro)

**Criticidade**: 🔴 Critical — protege contra criar Business ou User parcial.

1. Cadastrar "Barbearia do João", obtendo o slug `barbearia-do-joao`.
2. Iniciar um segundo cadastro com o mesmo nome, e-mail diferente.
3. Confirmar.

**Resultado esperado**: o cadastro é recusado com erro no campo do link,
sugerindo uma variação disponível. Nome, e-mail e slug digitados permanecem no
formulário. **Nenhum** `Business` ou `User` parcial é criado. A resposta é a
tela de cadastro com erro, nunca um erro interno.

### E2E Test Summary

| ID | Cenário | Tipo | Cobre |
|---|---|---|---|
| E2E-1 | Cadastro até o painel | Caminho feliz | US-1, US-2, BR-3, BR-4 |
| E2E-2 | Login com redirecionamento de volta | Fluxo alternativo | US-3 |
| E2E-3 | Slug em uso | Erro | US-2, BR-2 |

Um quarto cenário, não numerado por ser transversal a todas as features:
**isolamento entre tenants** — dois estabelecimentos cadastrados, e nenhuma
rota autenticada de um alcança dado do outro.

---

## Non-Functional Requirements

### Performance

Sem exigência específica. O cadastro acontece uma vez na vida do
estabelecimento, e o login algumas vezes por dia.

### Security

- Senha com BCrypt.
- Proteção CSRF nos formulários, conforme o ADR 0007.
- Mensagem de login que não revela existência de conta.
- `tenant_id` jamais aceito do cliente: vem sempre da sessão (ADR 0004).
- Senha nunca registrada em log — vale também para o log de requisição.
- Sessão invalidada no servidor ao sair.

### Usability

- Três campos no formulário. Cada campo a mais é abandono.
- O link público é visível antes de confirmar, não descoberto depois.
- Erro sempre no campo que o causou, preservando o restante do preenchimento.

### Scalability

Não se aplica ao escopo do MVP.

---

## Success Metrics

### Business Metrics

- Estabelecimentos que concluem o cadastro depois de iniciá-lo — **target: 100% no piloto**; qualquer abandono com n=1 indica defeito, não estatística.

### User Metrics

- Tempo entre abrir `/cadastro` e chegar ao painel — **target: menos de 90 segundos**, medido com o barbeiro piloto.
- Passos até estar autenticado pela primeira vez — **target: 1 formulário, 0 telas intermediárias**.

### Technical Metrics

- Estabelecimentos criados sem usuário dono, ou o inverso — **target: 0**, garantido pela transação única.
- Senhas em texto claro em banco ou log — **target: 0**.
- Acessos bem-sucedidos a dado de outro tenant — **target: 0**, verificado por teste automatizado.

---

## Dependencies

Capacidades necessárias, não tecnologias — a escolha concreta pertence à spec
técnica:

- Armazenamento relacional com garantia transacional.
- Autenticação por sessão e proteção contra falsificação de requisição.
- Hash de senha resistente a força bruta.
- Renderização de página no servidor.

Nenhuma dependência externa ao projeto. Nenhuma integração com terceiro.

---

## Risks

| Risco | Impacto | Mitigação |
|---|---|---|
| Slug escolhido no cadastro e arrependimento depois | Médio | O ADR 0011 já prevê troca com redirecionamento; fora do escopo desta feature, mas o modelo suporta |
| Sem confirmação de e-mail, alguém cadastra com e-mail de terceiro | Baixo no piloto | Aceito conscientemente; vira problema quando o cadastro for aberto ao público |
| Sem recuperação de senha, o dono perde o acesso | Alto se acontecer | TODO-109 no backlog; no piloto, a senha pode ser redefinida diretamente no banco |
| O dono termina o cadastro e não sabe o que fazer | Médio | O painel exibe o próximo passo explicitamente |

---

## Open Questions

Nenhuma. As quatro decisões em aberto foram fechadas na entrevista: campos
mínimos, slug derivado e editável, login automático após o cadastro, e o
primeiro profissional ficando para a TODO-002.

---

## Assumptions

- **Fuso horário** assume `America/Sao_Paulo`, sem perguntar. O campo existe no
  modelo e vira editável quando houver a tela de configuração.
- **Nome do usuário** começa igual ao nome do estabelecimento, por não ser
  pedido no formulário. Vira editável na mesma tela futura.
- **Papel do usuário** é sempre proprietário. Não há outro papel enquanto não
  houver segundo usuário.
- O estabelecimento fica sem nenhum profissional ao fim desta feature, e
  portanto sem agenda utilizável. É esperado: a TODO-002 vem em seguida.

---

## References

- [TODO-001](../../../backlog.md) — item de origem
- [ADR 0003](../../../../docs/architecture/adr/0003-identidade-dentro-de-organization.md) — identidade dentro de Organization; `Business` e `User` na mesma transação
- [ADR 0004](../../../../docs/architecture/adr/0004-multi-tenancy-por-discriminador.md) — `TenantContext` e resolução por sessão
- [ADR 0008](../../../../docs/architecture/adr/0008-rota-publica-com-prefixo.md) — `/b/{slug}` e o efeito sobre as palavras reservadas
- [ADR 0011](../../../../docs/architecture/adr/0011-ciclo-de-vida-dos-dados.md) — desativação lógica e histórico de slug
- [ADR 0012](../../../../docs/architecture/adr/0012-bootstrap-sem-build-com-tema-no-publico.md) — Bootstrap 5 no admin
- [Glossário](../../../../docs/domain/glossary.md) — `Business` é o tenant
- [Modelo de dados](../../../../docs/domain/data-model.md) — `Business`, `User`, `BusinessSlugHistory`
