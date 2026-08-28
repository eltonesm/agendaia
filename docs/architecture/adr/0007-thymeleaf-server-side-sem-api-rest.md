# ADR 0007 — Thymeleaf server-side, sem API REST e sem SPA

- **Status:** Aceito
- **Data:** 2026-08-28

## Contexto

O MVP precisa de duas interfaces: um painel administrativo para o dono e uma
página pública de agendamento acessada pelo navegador do celular do cliente
final. Não há aplicativo mobile no escopo.

Separar frontend e backend traria dois projetos, dois pipelines, dois deploys,
CORS, gestão de token no navegador e uma API REST completa a manter — para uma
equipe muito pequena validando produto numa VPS.

## Decisão

Nós vamos renderizar tudo no servidor com **Spring MVC + Thymeleaf**, num único
deploy. Thymeleaf fica restrito aos adapters de entrada web e não atravessa
para `application` nem para `domain`.

**Não haverá API REST no MVP.** Existirão apenas endpoints internos consumidos
pela própria página — na prática, um: os horários disponíveis para uma oferta e
uma data, devolvendo fragmento HTML.

Para a interação do seletor de horários, avaliar **HTMX** antes de escrever
`fetch` e manipulação de DOM à mão. Se JavaScript simples resolver bem, usar o
mais simples.

## Consequências

Um deploy, um container, um pipeline. A página pública fica indexável e rápida
no 3G do celular do cliente — que é o dispositivo real de uso.

Formulários HTML com Spring Security trazem proteção CSRF por padrão, sem
decidir onde guardar token nem como renová-lo.

Quando existir aplicativo mobile ou integração de terceiro, a API REST nasce
como um **adapter de entrada novo**, ao lado do web, sem tocar em nenhum caso
de uso. É precisamente para isso que a arquitetura hexagonal está ali.

**O que fica pior:** interações ricas custam mais que numa SPA, e a experiência
tem teto. Aceitável para agendar horário; insuficiente se um dia o produto
virar uma ferramenta de uso contínuo durante o expediente.

Qualquer documento anterior que defenda "API First" fica **contraditório com
esta decisão** e deve ser arquivado ou reescrito como "contratos internos" —
caso contrário, código gerado por IA vai produzir controllers REST duplicados
para cada tela.

## Gatilho de reavaliação

Aplicativo mobile, integração de terceiro, ou uma tela cuja interação se torne
inviável no modelo server-side.
