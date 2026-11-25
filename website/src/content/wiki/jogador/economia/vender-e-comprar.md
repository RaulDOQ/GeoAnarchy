---
title: Vender e comprar
summary: Como usar o menu do mercado e os comandos rápidos, e o que significa cada mensagem.
order: 4
---

Tudo nesta página só funciona **dentro do ponto de venda** de um mercado. Veja como achar um em [Mercados](/wiki/economia/mercados/).

## Pelo menu

Estando no mercado, digite:

```text
/mercado
```

Abre a lista de itens que aquele mercado negocia. Clique num item para negociar:

| Clique | O que faz |
|---|---|
| Botão esquerdo | Vende 1 |
| Shift + botão esquerdo | Vende todos os que você carrega |
| Botão direito | Compra 1 |
| Shift + botão direito | Compra uma pilha (64, na maioria dos itens) |

Na linha de baixo do menu:

- **Categoria**: filtra por tipo de item. Clique para ir à próxima, botão direito para voltar.
- **Vender tudo**: vende de uma vez tudo o que você carrega e o mercado aceita. Itens danificados, renomeados e encantados ficam com você.
- O ícone do meio mostra a moeda do mercado, o imposto e quanto dinheiro você tem.

## Lendo um item

Passe o mouse em cima de um item para ver:

| No jogo | Quer dizer |
|---|---|
| `Venda por 9.20 Geos` | Quanto o mercado paga por 1 unidade agora |
| `Compre por 10.58 Geos` | Quanto custa comprar 1 unidade agora |
| `Compra: sem estoque` | O mercado não tem esse item para vender |
| `Em estoque: 1,240 (normal 3,000)` | Quantos o mercado tem, e quanto seria o estoque normal |
| `Você tem 64: vale 560.12 Geos` | Quanto você recebe se vender todos os que carrega |
| `Cheio: não está comprando mais agora` | O mercado está cheio desse item e não compra mais por enquanto |
| `Matéria-prima` | Matéria-prima: comprar custa mais que o normal |

O preço muda a cada venda e a cada compra. O valor de "vender todos" já leva isso em conta. Entenda o motivo em [Como os preços funcionam](/wiki/economia/precos/).

## Pelos comandos

Para quem prefere digitar:

| Comando | O que faz |
|---|---|
| `/vender hand` | Vende a pilha que está na sua mão |
| `/vender all` | Vende tudo o que você carrega e o mercado aceita |
| `/vender diamond` | Vende todos os seus diamantes |
| `/vender diamond 10` | Vende 10 diamantes |
| `/comprar diamond` | Compra 1 diamante |
| `/comprar diamond 32` | Compra 32 diamantes |

`/vender`, `/comprar` e `/mercado` são os mesmos comandos em português: `/vender hand`, `/comprar diamond 32`.

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
| `Você não está em um mercado` | Você está fora do ponto de venda | Entre no anel de partículas |
| `O mercado ... não negocia ...` | Esse mercado não negocia esse item | Venda em outro mercado |
| `Os mercados não negociam ...` | Nenhum mercado negocia esse item | Não é um item vendável |
| `Você não tem ... comum` | O item está danificado, renomeado ou encantado | Só itens limpos são aceitos |
| `O mercado ... está cheio de ...` | O mercado já tem demais desse item | Espere, ou leve para outro mercado |
| `O mercado ... ficou sem dinheiro` | É um mercado de país e o tesouro acabou | Volte depois, ou venda em outro |
| `O mercado ... não tem ... no estoque` | Não tem esse item para comprar | Procure em outro mercado |
| `Você não tem dinheiro para isso` | Falta dinheiro **na moeda do mercado** | Confira com `/saldo`. Num mercado de país, troque Geos pela moeda dele |
| `Isso vale menos de 0.01` | O que você vendeu vale menos de um centavo | Venda mais unidades de uma vez |
| `Seu inventário está cheio` | Não cabe mais nada | Libere espaço |
| `O mercado ... está fechado` | O dono fechou o mercado | Volte depois |
