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
    next: 'Anarquia',
  },

  liveMap: {
    label: 'Mapa ao vivo do servidor GeoAnarchy. Use + e − para dar zoom e as setas para mover.',
    caption: 'Ao vivo: este é o mapa real do servidor',
    // Shown instead of the caption while the live map is down; {time} is when the
    // picture was taken.
    snapshotCaption: 'Mapa ao vivo fora do ar · imagem das {time}',
    offlineCaption: 'Mapa ao vivo fora do ar',
    loading: 'Carregando o mapa…',
    offline: 'O mapa está fora do ar no momento.',
    activate: 'Clique para explorar',
    hint: 'Role para dar zoom, arraste para mover',
    touchHint: 'Use dois dedos para dar zoom e mover',
  },

  anarchy: {
    chapter: 'Anarquia',
    title: ['Uma verdadeira', 'anarquia política.'],
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
    title: ['Baseado no', 'mundo real.'],
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
    title: ['Um servidor', 'realista.'],
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
        title: 'Uma verdadeira anarquia política',
        body: 'Um país pode atacar o outro quando quiser. Cada país pode ter o nome que quiser, onde quiser, ter suas próprias leis e filosofias.',
        href: '/#anarquia',
      },
      {
        title: 'Baseado no mundo real',
        body: 'O mundo do servidor é uma réplica do mundo real: continentes, terreno, mares e biomas. Cada bloco representa aproximadamente 1 km.',
        href: '/#mapa',
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
  store: {
    meta: {
      title: 'Loja | GeoAnarchy',
      description: 'A loja do GeoAnarchy ainda não abriu.',
    },
    chapter: 'Loja',
    title: ['A loja ainda', 'não abriu'],
    lead: 'Acompanhe as novidades no Discord.',
    cta: 'Entrar no Discord',
    note: {
      title: 'E dentro do jogo?',
      body: 'No GeoAnarchy, toda a economia é controlada por jogadores, sem loja do servidor.',
    },
  },

  // "GeoWiki". Three first articles, all taken from what the site already says.
  wiki: {
    meta: {
      title: 'GeoWiki | GeoAnarchy',
      description: 'O guia do GeoAnarchy: como entrar, o mundo do servidor e como ele funciona.',
    },
    chapter: 'GeoWiki',
    title: ['O guia do', 'GeoAnarchy'],
    lead: 'O essencial para começar. Mais páginas em breve.',
    indexLabel: 'Nesta página',
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
