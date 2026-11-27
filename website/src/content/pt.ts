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

  // "Sobre nós". The three ideas repeat the home page's own words; then the server's story,
  // told by its founders (Astrum and Drew), from 2024 to its return in 2026.
  about: {
    meta: {
      title: 'Sobre nós | GeoAnarchy',
      description: 'A história do GeoAnarchy: um servidor de geopolítica sem regras no mapa real da Terra, criado em 2024 por Astrum e Drew e de volta em 2026.',
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
    story: {
      chapter: 'Nossa história',
      title: 'De onde veio o GeoAnarchy',
      founders: 'Fundado por Astrum e Drew',
      // The numbers from the first GeoAnarchy, shown above the story.
      stats: [
        { value: '2024', label: 'O ano em que tudo começou' },
        { value: '150', label: 'Jogadores online ao mesmo tempo' },
        { value: '1.500+', label: 'Membros no Discord' },
      ],
      // Each part of the story: a year (or a short tag), a title and its paragraphs.
      parts: [
        {
          tag: '2024',
          title: 'Um servidor sem regras',
          body: [
            'O GeoAnarchy foi fundado em 2024 por nós dois, Astrum e Drew. A gente gostava de política, economia, países e Minecraft, mas todo servidor de geopolítica era a mesma coisa: regras de roleplay para seguir, tanques, armas e aviões para aprender a usar, e um PvP totalmente diferente do jogo. Ainda havia vários níveis de burocracia, quando a gente só queria entrar, criar o nosso país e atacar os outros.',
            'E nada daquilo parecia realista. No mundo real, o que impede um país de atacar outro são as consequências impostas pelos outros países, e não uma força divina, que nesses servidores eram o próprio servidor e as suas regras.',
            'Então decidimos criar o nosso próprio servidor, fugindo de tudo isso: um servidor de geopolítica sem regras, onde cada parte da estrutura é controlada pelos jogadores, igual no mundo real.',
          ],
        },
        {
          tag: 'A ideia',
          title: 'A base é o mundo real',
          body: [
            'Antigamente, o mundo era desorganizado. Países atacavam outros sem motivo, e piratas viviam de atacar quem encontravam e roubar tudo, só porque queriam. Hoje só existe “paz” porque vários países se uniram e criaram instituições.',
            'Essas instituições não são uma lei definitiva. Quando um país ataca outro sem motivo, ele não é banido da existência: os outros países se juntam contra ele, e ele sofre as consequências, inclusive econômicas.',
            'O mesmo pode acontecer no GeoAnarchy. Países que buscam paz podem se unir para se defender de piratas, terroristas e ataques sem motivo. Mas se alguém quiser atacar um país sem motivo, o servidor nunca vai impedir. Quem impede são os próprios jogadores.',
          ],
        },
        {
          tag: '2024',
          title: 'O crescimento',
          body: [
            'Por um bom tempo, o servidor teve uma base estável de jogadores e não parou de crescer. Chegamos a 150 jogadores online ao mesmo tempo e a mais de 1.500 membros no Discord.',
          ],
        },
        {
          tag: '2025',
          title: 'O fim',
          body: [
            'No início de 2025, decidimos fechar o servidor, por vários motivos. A escola tomava o nosso tempo, e o servidor não rendia o suficiente para se manter: só para ficar no ar, gastávamos mais de 200 reais por mês, sem contar a divulgação e as melhorias.',
            'Sem tempo e sem dinheiro, o servidor foi caindo aos poucos, até ficar com uma média de 15 jogadores online. Então decidimos encerrar.',
          ],
        },
        {
          tag: '2025',
          title: 'Outro servidor',
          body: [
            'Logo depois de fechar, vendemos o servidor para novos donos. Ele reabriu com outro nome e outras ideias: era focado em roleplay e queria adicionar coisas como tanques e armas.',
            'Depois de alguns meses, esse servidor morreu de vez, e nunca mais conseguimos trazer de volta o servidor que tínhamos construído.',
          ],
        },
        {
          tag: '2026',
          title: 'A volta',
          body: [
            'Em 2026, com a escola terminada, temos muito mais tempo para melhorar o servidor, administrar e investir nele. Por isso decidimos trazer o GeoAnarchy de volta, com a mesma ideia de sempre e com uma gameplay e uma qualidade muito melhores.',
          ],
        },
      ],
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
    // A plan also gives everything the plans below it give, so a list starts by saying so.
    benefits: {
      'geo-plus': [
        'Sentar, deitar e rastejar onde quiser (/sit, /lay, /crawl)',
        'Skins de itens: até 5 em uso, pacotes Silvestre e Glacial',
        '3 mensagens de abate personalizadas',
        '3 tags personalizadas',
      ],
      'geo-ultra': [
        'Tudo do Geo+',
        'Nome colorido, com degradês e cores RGB (/nome)',
        'Partículas em volta do personagem (/pp)',
        'Skins de itens: até 10 em uso, mais os pacotes Dracônico e Abissal',
        '5 mensagens de abate personalizadas',
        '3 mensagens de entrada personalizadas e coloridas',
        'Efeito de morte',
        '5 tags personalizadas',
      ],
      'geo-ultra-plus': [
        'Tudo do Geo Ultra',
        'Nomes personalizados nos itens',
        '20% a mais de XP',
        '20% a mais de XP de habilidades',
        'Skins de itens: até 18 em uso, mais o pacote Celestial',
        '10 mensagens de abate personalizadas e coloridas',
        '5 mensagens de morte personalizadas e coloridas',
        'Efeitos de abate e efeitos de morte',
        'Uma vez por mês: 30% a mais de XP para o servidor inteiro, por 30 minutos',
        '10 tags personalizadas',
      ],
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
      description: 'O guia do GeoAnarchy: como entrar, o mundo do servidor, as regras, a economia, os países, os encantamentos e o VIP.',
    },
    chapter: 'GeoWiki',
    title: ['O guia do', 'GeoAnarchy'],
    lead: 'O essencial para jogar: como entrar, as regras do mundo, a economia, os países, os encantamentos e o VIP.',
    // The side menu.
    indexLabel: 'Nesta wiki',
    // The number beside each section of the side menu, read aloud as "12 artigos".
    groupCount: '{n} artigos',
    // The switch between the two wikis and the tools, at the top of every wiki page.
    switchLabel: 'Escolha a wiki',
    wikis: {
      jogador: { label: 'Wiki do Jogador', note: 'Para quem joga' },
      pais: { label: 'Wiki do País', note: 'Para fundadores de países' },
      ferramentas: { label: 'Ferramentas', note: 'Cores e textos' },
    },
    // The side menu's groups. `start` is the front page's own articles; the others are the
    // folders in src/content/wiki/<wiki>/.
    sections: {
      start: 'Começando',
      geral: 'Mundo e regras',
      economia: 'Economia',
      paises: 'Países',
      encantamentos: 'Encantamentos',
      vip: 'VIP e skins',
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
    // The other sections listed on the players' front page, after the economy.
    countries: {
      id: 'paises',
      title: 'Países',
      intro: 'O que é um país, como entrar num, e o que você pode e não pode fazer no seu país, no dos aliados e no dos outros. Para fundar um país, veja a Wiki do País.',
    },
    enchantments: {
      id: 'encantamentos',
      title: 'Encantamentos',
      intro: '40 encantamentos novos, comprados com XP em Tomos Selados: como abrir, aplicar e juntar.',
    },
    vip: {
      id: 'vip',
      title: 'VIP e skins',
      intro: 'Os planos Geo+, Geo Ultra e Geo Ultra+, o GeoMenu (/geomenu), o nome colorido e as skins de itens.',
    },
    // The founders' wiki.
    // The tools tab (src/pages/wiki/ferramentas). Each tool is a page of its own.
    tools: {
      meta: {
        title: 'Ferramentas | GeoWiki',
        description: 'Ferramentas do GeoAnarchy: gerador de textos coloridos com degradê, cores RGB, negrito e sublinhado, nos formatos que o servidor usa.',
      },
      title: ['Ferramentas', 'do GeoAnarchy'],
      lead: 'Monte textos coloridos para o nome do seu país, o seu apelido e o chat, com degradês e cores RGB, e copie o código pronto.',
      menu: 'Ferramentas',
      colors: {
        menu: 'Gerador de cores',
        title: 'Gerador de cores',
        intro: 'Escreva o texto, escolha as cores e os estilos de cada parte, e copie o código no formato de onde você vai usar.',
        segment: 'Parte {n}',
        text: 'Texto',
        placeholder: 'Digite aqui',
        mode: 'Cor',
        modes: { solid: 'Uma cor', gradient: 'Degradê', rainbow: 'Arco-íris' },
        stop: 'Cor {n} do degradê',
        addStop: 'Mais uma cor',
        removeStop: 'Tirar a última cor',
        palette: 'Cores do Minecraft (aplica na cor selecionada)',
        styles: { bold: 'Negrito', italic: 'Itálico', underline: 'Sublinhado', strike: 'Riscado', obfuscated: 'Embaralhado' },
        addSegment: 'Adicionar parte',
        removeSegment: 'Remover esta parte',
        preview: 'Como fica no jogo',
        code: 'Código',
        copy: 'Copiar código',
        length: '{n} caracteres',
        countryLimit: 'Nomes de país aceitam até 32 caracteres, contando os códigos.',
        countryOver: 'Passou de 32 caracteres: o nome do país não vai caber. Use menos partes, cores ou estilos.',
        formats: [
          { id: 'mini', label: 'Nome de país', note: 'Para /pais create e /pais rename (formato MiniMessage)' },
          { id: 'amp', label: 'Chat e apelido', note: 'Para o /nome dos VIP e o chat (códigos & e &#RRGGBB)' },
          { id: 'bukkit', label: 'Outros plugins', note: 'Para plugins que só aceitam &x&R&R&G&G&B&B' },
        ],
        tips: [
          'Renomear um país custa 10.000 Geos e só pode ser feito uma vez por dia: confira a prévia antes.',
          'O /nome é dos planos VIP e só aceita o seu próprio nome: as cores mudam, as letras (e as maiúsculas) não.',
          'Quem joga pelo Bedrock (celular e console) vê as cores RGB um pouco diferentes.',
        ],
      },
    },
    country: {
      meta: {
        title: 'Wiki do País | GeoWiki',
        description: 'O guia para quem funda e governa um país no GeoAnarchy: o passo a passo, cargos, níveis, território, guerras, moeda própria, tesouro e mercado.',
      },
      title: ['O guia do', 'fundador'],
      lead: 'Para quem fundou um país ou quer fundar um: o passo a passo, território, guerras, moeda própria, tesouro e mercado.',
      intro: {
        id: 'para-quem',
        title: 'Para quem é esta wiki',
        body: 'Esta wiki é para quem quer fundar um país e para quem ajuda a governar um. Se você só quer entrar num país, comprar e vender, a Wiki do Jogador tem tudo o que você precisa.',
        needsLabel: 'Antes de começar',
        needs: [
          'Fundar um país custa 20.000 Geos. O Tutorial mostra cada passo, de /pais create até a primeira guerra.',
          'Muita coisa fica só com o Fundador até ele liberar para um cargo, no menu de cargos do país: comprar território, mexer no banco, subir de nível e a economia (a permissão aparece como “Manage economy”).',
          'O Geo do país fica no banco do país. Use /pais bank deposit para guardar dinheiro lá.',
        ],
      },
      countries: {
        id: 'paises',
        title: 'Seu país',
        intro: 'Fundar, os cargos, os 10 níveis, território e manutenção, e como funcionam as guerras.',
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
        'O Nether e o End estão fechados: os itens deles vêm dos mercados.',
        'Cristais do End e âncoras de renascimento são proibidos.',
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
