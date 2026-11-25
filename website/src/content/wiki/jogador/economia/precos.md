---
title: Como os preços funcionam
summary: Por que o mesmo item vale mais num mercado do que em outro, e por que o preço cai enquanto você vende.
order: 5
---

## A regra

> **Pouco estoque, preço alto. Muito estoque, preço baixo.**

Cada item tem um **preço normal** e um **estoque normal**. O mercado compara quanto ele tem com esse estoque normal:

- **Vazio:** paga **3 vezes** o preço normal.
- **No estoque normal:** paga o preço normal.
- **Acima do normal:** paga cada vez menos, até um preço mínimo.

## Um exemplo: diamantes

O preço normal do diamante é 100 Geos e o estoque normal é de 800 diamantes.

| Diamantes no mercado | O mercado paga por 1 |
|---|---|
| 0 (vazio) | 300 Geos |
| 400 | 150 Geos |
| 800 (normal) | 100 Geos |
| 1.600 | 60 Geos |
| 2.400 | 43 Geos |
| 4.000 | 27 Geos |
| 5.600 ou mais | 20 Geos (o mínimo) |

Cada mercado faz essa conta com o **próprio** estoque. Por isso o diamante pode valer 300 Geos num mercado e 60 em outro, no mesmo momento.

## O preço cai enquanto você vende

Cada unidade é paga pelo preço daquele instante. Quando você vende a primeira, o estoque sobe e a segunda já vale um pouco menos.

> **Exemplo:** a Maria chega num mercado vazio com 64 diamantes. O primeiro vale 300 Geos. O último vale 259. No total ela recebe cerca de **17.800 Geos**, e não 19.200 (64 × 300).

O menu já mostra o valor certo para a quantidade que você carrega, na linha `Você tem ... vale ...`.

Vender tudo de uma vez ou aos poucos, um clique de cada vez, dá o mesmo resultado. Não existe truque de dividir a venda.

## Comprar custa mais do que vender

No mesmo mercado e no mesmo instante, **comprar custa 15% a mais** do que o mercado paga na venda.

> **Exemplo:** o mercado paga 100 Geos por um diamante. Para comprar um, você paga 115.

Por isso vender e comprar de volta no mesmo mercado sempre dá prejuízo. O lucro está em comprar onde é barato e vender onde é caro: veja [Rotas de comércio](/wiki/economia/rotas-de-comercio/).

## Quando o mercado fica cheio

Um mercado para de comprar um item quando chega a **10 vezes o estoque normal** dele. Com diamantes, isso é 8.000 unidades. Aparece `Cheio: não está comprando mais agora`.

Ele volta a comprar quando o estoque baixar: porque alguém comprou, ou pelo desgaste explicado a seguir.

## O estoque em excesso some aos poucos

Nos **mercados continentais**, a cada 30 minutos uma parte do estoque que está **acima do normal** desaparece, como se o continente estivesse consumindo os itens.

- Itens baratos e comuns somem mais rápido (pedra, terra, areia).
- Itens valiosos somem devagar (diamante, esmeralda).
- Itens que o mercado fabrica, como a netherita, **nunca somem** (veja [Itens do Nether e do End](/wiki/economia/itens-do-nether-e-do-end/)).
- A quantidade varia um pouco a cada vez.
- O estoque que está **no normal ou abaixo nunca some**.

> **Exemplo:** um mercado com 2.000.000 de pedregulhos perde cerca de 40.000 a cada meia hora. Um mercado com 1.000 diamantes perde cerca de 1.

Isso faz os preços voltarem ao normal com o tempo, mesmo depois que alguém despejou uma montanha de itens.

Nos **mercados de países** nada some: o estoque pertence ao país.

## Matéria-prima e item processado

Um item que passou por fornalha, defumador ou fogueira vale mais do que a matéria-prima dele:

| Matéria-prima | Processado | Quanto a mais |
|---|---|---|
| Ferro bruto, 8,20 | Lingote de ferro, 10 | 22% |
| Ouro bruto, 16,40 | Lingote de ouro, 20 | 22% |
| Cobre bruto, 2,05 | Lingote de cobre, 2,50 | 22% |
| Carne crua, 1,50 | Carne assada, 1,90 | 27% |
| Tronco de carvalho, 1 | Carvão vegetal, 1,30 | 30% |
| Areia, 0,08 | Vidro, 0,10 | 25% |
| Pedregulho, 0,05 | Pedra, 0,06 | 20% |

Ou seja: **fundir e cozinhar antes de vender rende mais.**

Em troca, **comprar matéria-prima num mercado custa o mesmo que comprar o item processado**. O ferro bruto é vendido ao mercado por 8,20, mas custa 11,50 para comprar, igual ao lingote. Isso impede alguém de comprar ferro bruto, fundir e revender no mesmo mercado com lucro, sem ter minerado nada. No menu, esses itens aparecem marcados como `Raw material`.

## Mercados de países

Um mercado de país funciona do mesmo jeito, com três diferenças:

- **Os preços são na moeda do país.** Eles partem dos preços do mercado continental mais próximo, convertidos pelo câmbio. Se a moeda do país perde valor, os preços naquela moeda sobem.
- **O país pode ajustar.** Ele pode pagar mais ou menos por um item (de 50% a 200% do normal) e cobrar um imposto de até 10% em cada negociação.
- **O estoque normal é menor**, um quarto do continental. Um mercado de país enche e esvazia mais rápido.

## Resumo para ganhar mais

- Venda onde o item está em falta. Use `/market list` e clique num mercado para ver os preços de longe.
- Funda os minérios e cozinhe a carne antes de vender.
- Um mercado vazio paga o triplo. Ser o primeiro a abastecer um mercado novo compensa.
- Se o preço despencou, espere: nos mercados continentais ele se recupera sozinho.
