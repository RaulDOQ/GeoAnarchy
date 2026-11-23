// Every text on the site, in Brazilian Portuguese. Pages read from here, so the wording
// can change (or another language can be added) without touching the layout.
import { site } from '../config/site';

const n = (value: number) => value.toLocaleString('pt-BR');
const w = site.world;

export const pt = {
  meta: {
    title: 'GeoAnarchy | Minecraft survival no mapa da Terra',
    description:
      'GeoAnarchy é um servidor brasileiro de Minecraft survival no mapa real da Terra. Funde um país, conquiste território, forme alianças, negocie e declare guerra sem pedir permissão.',
  },

  common: {
    skip: 'Pular para o conteúdo',
    devBanner: 'Prévia do site, ainda em desenvolvimento.',
    navLabel: 'Seções',
    menu: 'Menu',
    nav: [
      { id: 'inicio', label: 'Início' },
      { id: 'servidor', label: 'O servidor' },
      { id: 'mapa', label: 'O mapa' },
      { id: 'entrar', label: 'Como entrar' },
      { id: 'duvidas', label: 'Dúvidas' },
    ],
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
    title: ['Funde um país', 'Dispute a Terra'],
    lead: 'Um servidor brasileiro de Minecraft survival no mapa real da Terra. Crie seu país, conquiste território, forme alianças, negocie e declare guerra. Aqui ninguém pede permissão.',
    ctaCopy: 'Copiar IP',
    ctaMap: 'Abrir o mapa',
    facts: ['Java e Bedrock', 'Survival', `Terra em escala ${w.scale}`],
    next: 'O servidor',
  },

  liveMap: {
    label: 'Mapa ao vivo do servidor GeoAnarchy',
    caption: 'Ao vivo: este é o mapa real do servidor',
    live: 'Ao vivo',
    loading: 'Carregando o mapa…',
    activate: 'Clique para explorar',
    activateTouch: 'Toque para explorar',
    hint: 'Arraste para mover, role para dar zoom',
    failed: 'O mapa não respondeu. Abra em outra aba:',
    open: 'Tela cheia',
    foot: `Terra · ${w.scale}`,
  },

  server: {
    chapter: 'O servidor',
    title: ['Anarquia política', 'de verdade'],
    lead: 'Um país pode atacar outro por qualquer motivo. Nenhuma guerra precisa de justificativa de roleplay nem de aprovação da staff: quem decide como responder são os outros jogadores.',
    pillars: [
      {
        icon: 'flag',
        title: 'Funde seu país',
        body: 'Escolha um lugar do mundo real, dê um nome ao seu país e comece a construir. Sozinho ou com amigos, a bandeira é sua.',
      },
      {
        icon: 'territory',
        title: 'Conquiste território',
        body: 'Reivindique terras e expanda suas fronteiras pelo mapa da Terra. O que é seu, você precisa conseguir manter.',
      },
      {
        icon: 'handshake',
        title: 'Alianças e comércio',
        body: 'Negocie com vizinhos, feche acordos e forme blocos. Diplomacia é feita pelos jogadores, do jeito que eles quiserem.',
      },
      {
        icon: 'swords',
        title: 'Guerra sem burocracia',
        body: 'Declarar guerra não exige motivo nem pedido à staff. Cada ataque tem consequência: o resto do mundo está olhando.',
      },
    ],
    illustrationNote: 'Ilustração',
  },

  world: {
    chapter: 'O mapa',
    title: ['A Terra inteira', `em escala ${w.scale}`],
    lead: 'O mundo do servidor é o planeta: continentes, oceanos e ilhas no lugar certo. Cada bloco representa cerca de um quilômetro do mundo real.',
    checks: [
      'Todos os continentes em um único mundo',
      'Subterrâneo novo sob todo o mapa, com minérios',
      'Fora da borda do mapa só existe o vazio',
    ],
    open: 'Abrir o mapa ao vivo',
    facts: [
      { value: `${n(w.width)} × ${n(w.depth)}`, label: 'blocos de leste a oeste e de norte a sul' },
      { value: w.scale, label: 'escala: 1 bloco ≈ 1 km' },
      { value: n(w.chunks), label: 'chunks jogáveis' },
    ],
    boundsLabel: 'Limites do mapa',
    bounds: [
      `X ${n(w.minX)} a ${n(w.maxX)}`,
      `Z ${n(w.minZ)} a ${n(w.maxZ)}`,
    ],
  },

  join: {
    chapter: 'Como entrar',
    title: ['Entre em', 'um minuto'],
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
        a: 'É um servidor brasileiro de Minecraft survival em um mapa real da Terra. Os jogadores criam países, conquistam território, formam alianças, fazem comércio e guerreiam entre si.',
      },
      {
        q: 'Preciso de permissão para declarar guerra?',
        a: 'Não. A marca do servidor é a anarquia política: um país pode atacar outro por qualquer motivo, sem justificativa de roleplay e sem aprovação da staff. Como o mundo reage é com os outros jogadores.',
      },
      {
        q: 'Dá para jogar no celular?',
        a: `Sim. Além do Java Edition, o servidor aceita o Bedrock Edition pelo endereço ${site.bedrock.address}, porta ${site.bedrock.port}.`,
      },
      {
        q: 'Qual é o tamanho do mapa?',
        a: `O mundo tem ${n(w.width)} por ${n(w.depth)} blocos, a Terra em escala ${w.scale}. Fora desse retângulo existe apenas o vazio.`,
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

  footer: {
    tagline: 'Minecraft survival no mapa real da Terra, com países, alianças e guerras decididas pelos jogadores.',
    serverTitle: 'Servidor',
    communityTitle: 'Comunidade',
    liveMap: 'Mapa ao vivo',
    status: 'Mapa ao vivo em',
    disclaimer: 'Não é um produto oficial do Minecraft. Não é aprovado nem associado à Mojang ou à Microsoft.',
  },
} as const;

export type Content = typeof pt;
