/*
 * Preenche o campo de link enquanto o nome do estabelecimento é digitado.
 *
 * ISTO É SUGESTÃO, NÃO REGRA (DD-4). O campo é editável, e quem valida o valor
 * submetido é o servidor — que confere formato, disponibilidade e palavra
 * reservada. Não existem duas derivações que precisem concordar: existe uma
 * sugestão aqui e uma validação lá.
 *
 * Por isso esta função pode divergir do SlugGenerator.java sem causar defeito:
 * o pior caso é o dono ver uma sugestão levemente diferente e ajustar.
 *
 * #preview-nome e #preview-link (redesenho-cadastro, 2026-09-10, DD-2):
 * o painel de marca do cadastro mostra o mesmo nome/link numa segunda
 * vitrine (mockup de link público). Não é uma segunda derivação — só
 * exibe, num lugar a mais, o valor que este script já calcula. Elementos
 * opcionais: se a página não tiver o painel de marca (ex.: uma tela
 * futura que reuse este script sem ele), os `if` abaixo simplesmente não
 * disparam.
 */
(function () {
  'use strict';

  const nome = document.getElementById('businessName');
  const slug = document.getElementById('slug');
  const previa = document.getElementById('slug-previa');
  const previewNome = document.getElementById('preview-nome');
  const previewLink = document.getElementById('preview-link');

  if (!nome || !slug) {
    return;
  }

  // Depois que o dono edita o link à mão, paramos de sobrescrever. Nada pior
  // que digitar o link e vê-lo ser apagado ao corrigir o nome.
  let editadoManualmente = slug.value.trim().length > 0;

  function derivar(texto) {
    return texto
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '')
      .slice(0, 60)
      .replace(/-+$/, '');
  }

  function atualizarPrevia() {
    const valorSlug = slug.value || 'seu-link';
    if (previa) {
      previa.textContent = valorSlug;
    }
    if (previewLink) {
      previewLink.textContent = 'simboraagendar.com.br/b/' + valorSlug;
    }
  }

  nome.addEventListener('input', function () {
    if (previewNome) {
      previewNome.textContent = nome.value || 'sua empresa';
    }
    if (!editadoManualmente) {
      slug.value = derivar(nome.value);
      atualizarPrevia();
    }
  });

  slug.addEventListener('input', function () {
    editadoManualmente = slug.value.trim().length > 0;
    atualizarPrevia();
  });

  atualizarPrevia();
})();
