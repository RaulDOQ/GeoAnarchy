// Every text on the site, in Brazilian Portuguese. Pages read from here, so the wording
// can change (or another language can be added) without touching the layout.
import { site } from '../config/site';

const n = (value: number) => value.toLocaleString('pt-BR');
const w = site.world;

export const pt = {
  meta: {
    title: 'GeoAnarchy | Geopolítica sem regras',
    description:
      'Um servidor de Minecraft brasileiro que se passa no mapa real da Terra. Crie sua nação, forme alianças, declare guerras, conquiste territórios. No GeoAnarchy, quem controla são os jogadores.',
  },

  common: {
    skip: 'Pular para o conteúdo',
    devBanner: 'Prévia do site, ainda em desenvolvimento.',
    navLabel: 'Seções',
    menu: 'Menu',
    // The header tabs: one per page.
    nav: [
      { id: 'home', label: 'Início', href: '/' },
      { id: 'about', label: 'Sobre nós', href: '/sobre/' },
      { id: 'store', label: 'Loja', href: '/loja/' },
      { id: 'wiki', label: 'GeoWiki', href: '/wiki/' },
    ],
    soon: 'Em breve',
    discord: 'Discord',
    play: 'Jogar',
    copy: 'Copiar',
    copied: 'Copiado',
    copiedToast: 'Endereço copiado: {value}',
    copyFailedToast: 'Não deu para copiar. O endereço é {value}',
  },

  hero: {
    announceTag: 'Ao vivo',
    announce: 'Veja o mapa em tempo real',
    facts: ['Minecraft survival', 'Java e Bedrock', `Terra em escala ${w.scale}`],
    title: ['Geopolítica', 'Sem regras'],
    lead: 'Um servidor de Minecraft brasileiro que se passa no mapa real da Terra. Crie sua nação, forme alianças, declare guerras, conquiste territórios. No GeoAnarchy, quem controla são os jogadores.',
    ctaCopy: 'Copiar IP',
    ctaMap: 'Abrir o mapa',
    next: 'O mapa',
  },

  liveMap: {
    label: 'Mapa ao vivo do servidor GeoAnarchy. Use + e − para dar zoom e as setas para mover.',
    caption: 'Ao vivo: este é o mapa real do servidor',
    // Shown instead of the caption while the live map is down; {time} is when the
    // picture was taken.
    snapshotCaption: 'Mapa ao vivo fora do ar · imagem das {time}',
    offlineCaption: 'Mapa ao vivo fora do ar',
    // Shown while a visitor is zoomed in and the card shows the site's saved copy of
    // the map instead of Dynmap's page (only when the map's tiles could not be used).
    zoomedCaption: 'Ampliado: cópia do mapa guardada pelo site',
    loading: 'Carregando o mapa…',
    offline: 'O mapa está fora do ar no momento.',
    activate: 'Clique para explorar',
    hint: 'Role para dar zoom, arraste para mover',
    touchHint: 'Use dois dedos para dar zoom e mover',
  },

  anarchy: {
    chapter: 'Anarquia',
    title: ['Uma verdadeira', 'anarquia política'],
    lead: 'Um país pode atacar o outro quando quiser. Cada país pode ter o nome que quiser, onde quiser, ter suas próprias leis e filosofias. O servidor não impõe nenhuma regra de roleplay.',
    paths: [
      {
        question: 'Quer um país focado em guerra e caos?',
        answer: 'No GeoAnarchy você pode.',
      },
      {
        question: 'Quer estabelecer ordem e paz no servidor inteiro, criando instituições e unindo países?',
        answer: 'No GeoAnarchy tudo isso é possível e permitido.',
      },
    ],
  },

  world: {
    chapter: 'O mapa',
    title: ['Baseado no', 'mundo real'],
    lead: 'O mundo do servidor é uma réplica do mundo real: continentes, terreno, mares e biomas. Cada bloco no servidor representa aproximadamente 1 km.',
    open: 'Abrir o mapa ao vivo',
    figureLabel: 'Mapa-múndi desenhado em blocos, com os limites do mundo do servidor em coordenadas do jogo.',
    sheet: `Terra · ${w.scale}`,
    origin: 'X 0 · Z 0',
    axisX: [n(w.minX), n(w.minX / 2), '0', n((w.maxX + 1) / 2), n(w.maxX)],
    axisZ: [n(w.minZ), '0', n(w.maxZ)],
    facts: [
      { value: `${n(w.width)} × ${n(w.depth)}`, label: 'blocos de leste a oeste e de norte a sul' },
      { value: '1 bloco ≈ 1 km', label: `escala ${w.scale}` },
      { value: n(w.chunks), label: 'chunks jogáveis' },
    ],
  },

  realism: {
    chapter: 'Realismo',
    title: ['Um servidor', 'realista'],
    lead: 'No nosso servidor, a fundação inteira é o próprio mapa e os jogadores.',
    points: [
      {
        label: 'Economia',
        body: 'No GeoAnarchy, toda a economia é controlada por jogadores, sem loja do servidor.',
      },
      {
        label: 'Viagem',
        body: 'No GeoAnarchy, também não temos nenhum comando de teleporte. Precisa atacar um país longe? Precisará organizar a logística de uma longa viagem, passando por desertos, mares, montanhas e mais.',
      },
    ],
    route: {
      label: 'Uma viagem longa no mapa: do seu país até o alvo, cruzando mar, deserto e montanhas.',
      from: 'Seu país',
      to: 'Alvo',
      sea: 'Mar',
      desert: 'Deserto',
      mountains: 'Montanhas',
    },
    commandsLabel: 'Não existe aqui',
    commands: ['/tpa', '/home', '/warp'],
  },

  vanilla: {
    chapter: 'Vanilla',
    title: ['Servidor sem', '“Modern slop”'],
    lead: 'No GeoAnarchy, a experiência é vanilla: o Minecraft que você já conhece.',
    // The things other servers add and this one does not, shown crossed out.
    goneLabel: 'Não existe aqui',
    gone: ['Tanques', 'Aviões', 'Armas de fogo'],
    goneNote: 'Coisas assim estragam a experiência do Minecraft, então ficam de fora.',
    points: [
      {
        label: 'Vanilla',
        body: 'A jogabilidade é a que você já está acostumado. Quem conhece o Minecraft já sabe jogar aqui.',
      },
      {
        label: 'Plugins',
        body: 'Usamos alguns plugins que melhoram a jogabilidade e acrescentam novidades, mas nada que fuja muito do que você já conhece.',
      },
    ],
  },

  join: {
    chapter: 'Como entrar',
    title: ['Quer participar?'],
    lead: 'O mesmo mundo para quem joga no Java Edition ou no Bedrock Edition. Copie o endereço e conecte.',
    java: {
      name: 'Java Edition',
      note: `Versão ${site.java.minVersion} ou mais nova · recomendada ${site.java.serverVersion}`,
      addressLabel: 'Endereço do servidor',
      steps: ['Abra o Minecraft e vá em Multijogador', 'Clique em Adicionar servidor', 'Cole o endereço e entre'],
    },
    bedrock: {
      name: 'Bedrock Edition',
      note: 'Celular, tablet e Windows',
      addressLabel: 'Endereço do servidor',
      portLabel: 'Porta',
      steps: ['Abra o Minecraft e vá em Jogar, depois Servidores', 'Clique em Adicionar servidor', 'Preencha o endereço e a porta e entre'],
    },
  },

  faq: {
    chapter: 'Dúvidas',
    title: ['Perguntas'],
    lead: 'Não achou a resposta?',
    more: 'Pergunte no Discord',
    items: [
      {
        q: 'O que é o GeoAnarchy?',
        a: 'É um servidor de Minecraft brasileiro que se passa no mapa real da Terra. Os jogadores criam nações, formam alianças, declaram guerras e conquistam territórios.',
      },
      {
        q: 'Preciso de permissão para declarar guerra?',
        a: 'Não. Um país pode atacar o outro quando quiser, e o servidor não impõe nenhuma regra de roleplay.',
      },
      {
        q: 'Tem teleporte ou loja do servidor?',
        a: 'Não. Não existe nenhum comando de teleporte, e toda a economia é controlada pelos jogadores, sem loja do servidor.',
      },
      {
        q: 'Dá para jogar no celular?',
        a: `Sim. Além do Java Edition, o servidor aceita o Bedrock Edition pelo endereço ${site.bedrock.address}, porta ${site.bedrock.port}.`,
      },
      {
        q: 'Qual é o tamanho do mapa?',
        a: `O mundo tem ${n(w.width)} por ${n(w.depth)} blocos, e cada bloco representa aproximadamente 1 km do mundo real.`,
      },
      {
        q: 'O mapa do site é mesmo ao vivo?',
        a: `Sim. Ele vem direto do servidor e mostra o mundo como está agora. Você também pode abri-lo em tela cheia em ${site.map.host}.`,
      },
    ],
  },

  closing: {
    title: ['A Terra está em jogo', 'Qual país é o seu?'],
    note: 'Servidor brasileiro · Survival · Mapa da Terra',
  },

  // "Sobre nós". The three ideas repeat the home page's own words; the rest of the page
  // is waiting for the server's story and team.
  about: {
    meta: {
      title: 'Sobre nós | GeoAnarchy',
      description: 'O que é o GeoAnarchy: um servidor de Minecraft brasileiro no mapa real da Terra, onde quem controla são os jogadores.',
    },
    chapter: 'Sobre nós',
    title: ['Quem controla', 'são os jogadores'],
    lead: 'O GeoAnarchy é um servidor de Minecraft brasileiro que se passa no mapa real da Terra. Crie sua nação, forme alianças, declare guerras, conquiste territórios.',
    pillars: [
      {
        title: 'Baseado no mundo real',
        body: 'O mundo do servidor é uma réplica do mundo real: continentes, terreno, mares e biomas. Cada bloco representa aproximadamente 1 km.',
        href: '/#mapa',
      },
      {
        title: 'Uma verdadeira anarquia política',
        body: 'Um país pode atacar o outro quando quiser. Cada país pode ter o nome que quiser, onde quiser, ter suas próprias leis e filosofias.',
        href: '/#anarquia',
      },
      {
        title: 'Um servidor realista',
        body: 'Toda a economia é controlada por jogadores, sem loja do servidor, e não temos nenhum comando de teleporte.',
        href: '/#realismo',
      },
    ],
    more: 'Ver na página inicial',
    soon: {
      title: 'Esta página ainda está sendo escrita',
      body: 'A história do servidor e a equipe por trás do GeoAnarchy vão aparecer aqui.',
    },
  },

  // "Loja". Nothing is on sale yet, so the page only says so.
  // "Loja". The plans and prices are in src/config/store.ts; this is the text around them.
  store: {
    meta: {
      title: 'Loja | GeoAnarchy',
      description: 'Ajude o GeoAnarchy a continuar e crescer: a meta mensal do servidor e os planos Geo+, Geo Ultra e Geo Ultra+.',
    },
    chapter: 'Loja',
    title: ['Ajude o servidor', 'a continuar e crescer'],
    lead: 'Cada plano ajuda a pagar os custos do servidor. Pague por mês, a cada 3 meses ou a cada 6 meses: quanto mais meses, menor o preço.',
    // The monthly goal (the amounts are in src/config/store.ts). {goal} is the goal,
    // {left} what is still missing, {over} what came in beyond it.
    goal: {
      label: 'Meta mensal',
      of: 'de {goal}',
      left: 'Faltam {left} para completar a meta',
      reached: 'Meta do mês completa',
      over: '{over} acima da meta',
      about: 'A meta é o que precisamos por mês para manter o servidor no ar.',
      beyond: 'Se a arrecadação passar da meta, todo o valor a mais será investido de volta no servidor: mais recursos, mais jogadores e mais novidades.',
    },
    // The switch over the plans: how often to pay. One name per period in
    // src/config/store.ts, in the same order.
    periodLabel: 'Forma de pagamento',
    periods: ['Mensal', '3 meses', '6 meses'],
    perMonth: 'por mês',
    // Under the price. {total} is what is charged each time, {months} how many months
    // that pays for, {full} the same months without the discount.
    billedMonthly: 'Cobrado todo mês',
    billed: '{total} a cada {months} meses',
    instead: 'em vez de {full}',
    // What each plan gives, by the plan's id in src/config/store.ts: one line of text
    // per benefit. While a list is empty the card says that the benefits come later.
    benefitsLabel: 'Benefícios',
    benefitsSoon: 'Os benefícios deste plano serão anunciados em breve.',
    benefits: {
      'geo-plus': [],
      'geo-ultra': [],
      'geo-ultra-plus': [],
    } as Record<string, readonly string[]>,
    status: {
      title: 'Os planos ainda não estão à venda',
      body: 'Avisaremos no Discord quando as assinaturas abrirem.',
    },
    cta: 'Entrar no Discord',
    note: {
      title: 'E dentro do jogo?',
      body: 'No GeoAnarchy, toda a economia é controlada por jogadores, sem loja do servidor.',
    },
  },

  // "GeoWiki". Two wikis: one for players (this text, plus the articles in
  // src/content/wiki/jogador) and one for country founders (`country` below, plus
  // src/content/wiki/pais). The long articles are Markdown files; the texts around them
  // are here.
  wiki: {
    meta: {
      title: 'GeoWiki | GeoAnarchy',
      description: 'O guia do GeoAnarchy: como entrar, o mundo do servidor, como ele funciona e a economia.',
    },
    chapter: 'GeoWiki',
    title: ['O guia do', 'GeoAnarchy'],
    lead: 'O essencial para jogar: como entrar, o mundo do servidor e a economia.',
    // The side menu.
    indexLabel: 'Nesta wiki',
    // The switch between the two wikis, at the top of every wiki page.
    switchLabel: 'Escolha a wiki',
    wikis: {
      jogador: { label: 'Wiki do Jogador', note: 'Para quem joga' },
      pais: { label: 'Wiki do País', note: 'Para fundadores de países' },
    },
    // The side menu's groups. `start` is the front page's own articles; the others are the
    // folders in src/content/wiki/<wiki>/.
    sections: {
      start: 'Começando',
      economia: 'Economia',
    },
    // An article page: its browser title, and the links at its end.
    articleTitle: '{title} | GeoWiki',
    previous: 'Anterior',
    next: 'Próximo',
    // The list of economy articles on each wiki's front page.
    economy: {
      id: 'economia',
      title: 'Economia',
      intro: 'O Geo, as moedas dos países, o câmbio e os mercados: como o dinheiro funciona no servidor.',
    },
    // The founders' wiki.
    country: {
      meta: {
        title: 'Wiki do País | GeoWiki',
        description: 'O guia para quem fundou um país no GeoAnarchy: moeda própria, tesouro, impressão de dinheiro e mercado.',
      },
      title: ['O guia do', 'fundador'],
      lead: 'Para quem fundou um país ou quer fundar um: moeda própria, tesouro e mercado.',
      intro: {
        id: 'para-quem',
        title: 'Para quem é esta wiki',
        body: 'Esta wiki é para os donos de países e para os membros que cuidam da economia do país. Se você só quer jogar, comprar e vender, a Wiki do Jogador tem tudo o que você precisa.',
        needsLabel: 'Antes de começar',
        needs: [
          'Você precisa ter um país, criado com /lands create.',
          'Só o dono do país mexe na economia dele. O dono pode dar essa permissão a um cargo no menu de cargos do país (a permissão aparece como “Manage economy”).',
          'O Geo do país fica no banco do país. Use /lands deposit para guardar dinheiro lá.',
        ],
      },
      economy: {
        id: 'economia',
        title: 'Economia do país',
        intro: 'Do banco do país até um mercado funcionando: leia na ordem se for a sua primeira vez.',
      },
    },
    join: {
      id: 'como-entrar',
      title: 'Como entrar',
      intro: 'O mesmo mundo para quem joga no Java Edition ou no Bedrock Edition. Copie o endereço e conecte.',
    },
    world: {
      id: 'o-mundo',
      title: 'O mundo',
      intro: 'O mundo do servidor é uma réplica do mundo real: continentes, terreno, mares e biomas.',
      rows: [
        { label: 'Tamanho', value: `${n(w.width)} × ${n(w.depth)} blocos` },
        { label: 'Escala', value: '1 bloco ≈ 1 km' },
        { label: 'Limites em X', value: `${n(w.minX)} a ${n(w.maxX)}` },
        { label: 'Limites em Z', value: `${n(w.minZ)} a ${n(w.maxZ)}` },
        { label: 'Mapa ao vivo', value: site.map.host },
      ],
    },
    rules: {
      id: 'como-funciona',
      title: 'Como o servidor funciona',
      intro: 'No GeoAnarchy, quem controla são os jogadores.',
      items: [
        'Um país pode atacar o outro quando quiser.',
        'O servidor não impõe nenhuma regra de roleplay.',
        'Cada país pode ter o nome que quiser, onde quiser, e ter suas próprias leis e filosofias.',
        'Toda a economia é controlada por jogadores, sem loja do servidor.',
        'Não existe nenhum comando de teleporte.',
      ],
    },
  },

  notFound: {
    meta: {
      title: 'Página não encontrada | GeoAnarchy',
      description: 'Este endereço não existe.',
    },
    chapter: 'Erro 404',
    title: ['Página não', 'encontrada'],
    lead: 'Este endereço não existe no site.',
    back: 'Voltar ao início',
  },

  footer: {
    tagline: 'Um servidor de Minecraft brasileiro no mapa real da Terra. Quem controla são os jogadores.',
    siteTitle: 'Site',
    serverTitle: 'Servidor',
    communityTitle: 'Comunidade',
    liveMap: 'Mapa ao vivo',
    status: 'Mapa ao vivo em',
    disclaimer: 'Não é um produto oficial do Minecraft. Não é aprovado nem associado à Mojang ou à Microsoft.',
  },
} as const;

export type Content = typeof pt;
