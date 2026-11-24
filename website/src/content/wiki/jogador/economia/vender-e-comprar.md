---
title: Vender e comprar
summary: Como usar o menu do mercado e os comandos rápidos, e o que significa cada mensagem.
order: 4
---

Tudo nesta página só funciona **dentro do ponto de venda** de um mercado. Veja como achar um em [Mercados](/wiki/economia/mercados/).

## Pelo menu

Estando no mercado, digite:

```text
/market
```

Abre a lista de itens que aquele mercado negocia. Clique num item para negociar:

| Clique | O que faz |
|---|---|
| Botão esquerdo | Vende 1 |
| Shift + botão esquerdo | Vende todos os que você carrega |
| Botão direito | Compra 1 |
| Shift + botão direito | Compra uma pilha (64, na maioria dos itens) |

Na linha de baixo do menu:

- **Category** (categoria): filtra por tipo de item. Clique para ir à próxima, botão direito para voltar.
- **Sell everything** (vender tudo): vende de uma vez tudo o que você carrega e o mercado aceita. Itens danificados, renomeados e encantados ficam com você.
- O ícone do meio mostra a moeda do mercado, o imposto e quanto dinheiro você tem.

## Lendo um item

Passe o mouse em cima de um item para ver:

| No jogo | Quer dizer |
|---|---|
| `Sell for 9.20 Geos` | Quanto o mercado paga por 1 unidade agora |
| `Buy for 10.58 Geos` | Quanto custa comprar 1 unidade agora |
| `Buy: none in stock` | O mercado não tem esse item para vender |
| `In stock: 1,240 (normal 3,000)` | Quantos o mercado tem, e quanto seria o estoque normal |
| `You carry 64: worth 560.12 Geos` | Quanto você recebe se vender todos os que carrega |
| `Full: not buying more right now` | O mercado está cheio desse item e não compra mais por enquanto |
| `Raw material` | Matéria-prima: comprar custa mais que o normal |

O preço muda a cada venda e a cada compra. O valor de "vender todos" já leva isso em conta. Entenda o motivo em [Como os preços funcionam](/wiki/economia/precos/).

## Pelos comandos

Para quem prefere digitar:

| Comando | O que faz |
|---|---|
| `/sell hand` | Vende a pilha que está na sua mão |
| `/sell all` | Vende tudo o que você carrega e o mercado aceita |
| `/sell diamond` | Vende todos os seus diamantes |
| `/sell diamond 10` | Vende 10 diamantes |
| `/buy diamond` | Compra 1 diamante |
| `/buy diamond 32` | Compra 32 diamantes |

Os nomes dos itens são os do Minecraft em inglês, com `_` no lugar do espaço: `iron_ingot`, `oak_log`, `cooked_beef`. Aperte **Tab** enquanto digita para o jogo completar o nome.

## Se você pedir mais do que dá

O mercado faz o que for possível, em vez de recusar tudo:

- Pediu para comprar 64, mas só tem dinheiro para 20? Ele vende 20.
- Pediu para comprar 64, mas o estoque tem 10? Ele vende 10.
- Seu inventário só tem espaço para 5? Ele vende 5.
- Num mercado de país, o tesouro só consegue pagar 12 dos seus 64 itens? Ele compra 12.

A mensagem final sempre diz quantos itens foram negociados e por quanto.

## O que as mensagens querem dizer

| Mensagem | O que aconteceu | O que fazer |
|---|---|---|
| `You aren't at a market` | Você está fora do ponto de venda | Entre no anel de partículas |
| `The ... market doesn't trade ...` | Esse mercado não negocia esse item | Venda em outro mercado |
| `Markets don't trade ...` | Nenhum mercado negocia esse item | Não é um item vendável |
| `You don't carry any plain ...` | O item está danificado, renomeado ou encantado | Só itens limpos são aceitos |
| `The ... market is full of ...` | O mercado já tem demais desse item | Espere, ou leve para outro mercado |
| `The ... market has run out of money` | É um mercado de país e o tesouro acabou | Volte depois, ou venda em outro |
| `The ... market has no ... in stock` | Não tem esse item para comprar | Procure em outro mercado |
| `You can't afford that` | Falta dinheiro **na moeda do mercado** | Confira com `/bal`. Num mercado de país, troque Geos pela moeda dele |
| `That is worth less than 0.01` | O que você vendeu vale menos de um centavo | Venda mais unidades de uma vez |
| `Your inventory is full` | Não cabe mais nada | Libere espaço |
| `The ... market is closed` | O dono fechou o mercado | Volte depois |
