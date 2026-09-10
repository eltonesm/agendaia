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
 */
(function () {
  'use strict';

  const nome = document.getElementById('businessName');
  const slug = document.getElementById('slug');
  const previa = document.getElementById('slug-previa');

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
    if (previa) {
      previa.textContent = slug.value || 'seu-link';
    }
  }

  nome.addEventListener('input', function () {
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
