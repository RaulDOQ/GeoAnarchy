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
    nav: [
      { id: 'inicio', label: 'Início' },
      { id: 'anarquia', label: 'Anarquia' },
      { id: 'mapa', label: 'O mapa' },
      { id: 'realismo', label: 'Realismo' },
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
    facts: ['Minecraft survival', 'Java e Bedrock', `Terra ${w.scale}`],
    title: ['Geopolítica', 'Sem regras'],
    lead: 'Um servidor de Minecraft brasileiro que se passa no mapa real da Terra. Crie sua nação, forme alianças, declare guerras, conquiste territórios. No GeoAnarchy, quem controla são os jogadores.',
    ctaCopy: 'Copiar IP',
    ctaMap: 'Abrir o mapa',
    next: 'Anarquia',
  },

  liveMap: {
    label: 'Mapa ao vivo do servidor GeoAnarchy',
    caption: 'Ao vivo: o mapa real do servidor',
    live: 'Ao vivo',
    loading: 'Carregando o mapa…',
    activate: 'Clique para explorar',
    activateTouch: 'Toque para explorar',
    hint: 'Arraste para mover, role para dar zoom',
    failed: 'O mapa não respondeu. Abra em outra aba:',
    open: 'Tela cheia',
    foot: `Terra · ${w.scale}`,
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

  footer: {
    tagline: 'Um servidor de Minecraft brasileiro no mapa real da Terra. Quem controla são os jogadores.',
    serverTitle: 'Servidor',
    communityTitle: 'Comunidade',
    liveMap: 'Mapa ao vivo',
    status: 'Mapa ao vivo em',
    disclaimer: 'Não é um produto oficial do Minecraft. Não é aprovado nem associado à Mojang ou à Microsoft.',
  },
} as const;

export type Content = typeof pt;
