---
title: Como o mercado define os preços
summary: O preço no mercado do país vem de quatro coisas. Entender cada uma ajuda a não ser pego de surpresa.
order: 10
---

O preço de um item no mercado do país é montado em quatro passos.

## 1. O preço do continente

O mercado do país **segue o mercado continental mais próximo**. O ponto de partida é o preço daquele item nesse continente, em Geos.

- Se o continente está **cheio** do item, o preço de partida é baixo.
- Se o continente está **vazio**, o preço de partida é alto.
- Se o continente **não negocia** o item, o ponto de partida é o preço normal do item.

O mercado usa um preço **suavizado** do continente, que acompanha o real com cerca de 30 minutos de atraso. Assim ninguém consegue encher ou esvaziar o continente por um instante só para mexer no seu mercado.

Para saber qual continente o seu mercado segue, veja em `/mercado lista`: a linha `Segue o mercado ...`.

## 2. O ajuste do país

Em seguida entra a porcentagem que você definiu com `/mercado preco`, de 50% a 200%. Sem ajuste, vale 100%.

## 3. O estoque do próprio mercado

Depois o mercado olha o **próprio** estoque, com a mesma regra de todos os mercados:

- vazio: até 3 vezes o preço de partida;
- no estoque normal: o preço de partida;
- acima do normal: cada vez menos, até o mínimo.

O **estoque normal** de um mercado de país é **um quarto** do estoque normal de um mercado continental. Para diamantes, são 200 em vez de 800. Um mercado de país enche e esvazia mais rápido.

O preço nunca passa de 3 vezes o preço normal do item (vezes o ajuste do país), mesmo que o continente e o seu mercado estejam vazios ao mesmo tempo.

## 4. A conversão para a moeda do país

Por fim, o valor em Geos é convertido para a sua moeda pela taxa de câmbio (também suavizada, com cerca de 30 minutos de atraso).

Em cima disso entram os 15% a mais na compra e o imposto do país, se houver.

## Um exemplo completo

A Espanha tem um mercado em Madri que segue o mercado da Europa. O Peso está a 10 por Geo.

| Passo | Conta | Resultado |
|---|---|---|
| Lingote de ferro na Europa, com estoque normal | preço normal | 10 Geos |
| Ajuste da Espanha para ferro | 150% | 15 Geos |
| Madri tem metade do estoque normal de ferro | × 1,5 | 22,5 Geos |
| Conversão para Pesos | × 10 | **225 Pesos** |

Madri paga 225 Pesos por um lingote. Para comprar um, o jogador paga cerca de 259 Pesos (15% a mais).

Agora mude uma coisa de cada vez:

- **A Europa enche de ferro** e o preço lá cai para 6 Geos. Madri passa a pagar 135 Pesos.
- **O Peso cai para 15 por Geo.** Madri passa a pagar 337 Pesos pelo mesmo lingote. Em Geos é o mesmo valor, mas o tesouro gasta mais Pesos.
- **Madri enche de ferro** até o dobro do normal. O preço cai para 90 Pesos.

## O que isso significa para o país

**Você não controla tudo.** O continente e o câmbio mexem nos seus preços sozinhos. O que você controla é o ajuste por item, o imposto, a lista de itens e o estoque.

**Um mercado vazio paga caro.** Com o estoque zerado, o tesouro paga até o triplo. Os primeiros dias de um mercado são os mais caros para o tesouro.

**Moeda fraca, mercado caro.** Se a sua moeda perde valor, o tesouro precisa de mais unidades para comprar a mesma coisa. Imprimir para cobrir isso piora o problema.

**O estoque é uma ferramenta.** Retirar itens com `/mercado retirar` sobe o preço e atrai vendedores. Deixar o estoque encher baixa o preço e atrai compradores.

## Conferir os preços

Abra `/mercado` no ponto de venda, ou clique no seu mercado em `/mercado lista`. O menu mostra, para cada item, quanto o mercado paga, quanto cobra, o estoque atual e o estoque normal.

Vale olhar depois de qualquer mudança: um ajuste novo, uma impressão de dinheiro, uma guerra que mexeu no câmbio.
