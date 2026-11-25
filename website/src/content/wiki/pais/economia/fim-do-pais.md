---
title: Se o país acabar
summary: O que acontece com a moeda, o tesouro e o mercado quando um país é apagado.
order: 12
---

Um país acaba quando o dono o apaga, ou quando o servidor o remove porque o dono ficou tempo demais sem entrar e nenhum membro ativo pôde assumir. Faltar dinheiro para a manutenção tira chunks do país, mas não o apaga. A economia dele não some junto: uma parte fica, outra não.

## O que acontece

| | Depois que o país acaba |
|---|---|
| **A moeda** | Continua existindo, mas **congelada**: ninguém mais pode imprimir |
| **O dinheiro dos jogadores** | Fica na carteira de cada um, intacto |
| **O câmbio** | Continua funcionando. A moeda ainda pode ser trocada por Geos e por outras moedas |
| **Os mercados do país** | São **removidos**, com todo o estoque |
| **Os países que adotaram a moeda** | Continuam usando |
| **Pedidos de adoção em espera** | São cancelados |
| **Novas adoções** | Não são mais possíveis |

## A moeda congelada

Uma moeda sem país ainda vale o que o câmbio disser. A reserva de câmbio continua lá, com os Geos dela, e os jogadores podem trocar à vontade.

O que muda:

- **O total da moeda nunca mais aumenta.** Sem impressão, não há mais inflação causada por um governo.
- **Não há mais mercado** que negocie nela. Com menos lugares para gastar, a tendência é os jogadores trocarem por Geos, e o valor cair.

No jogo, ela aparece com o aviso `Its country no longer exists`.

## O que se perde

- **O estoque dos mercados.** Tudo o que estava guardado some com o mercado.
- **O que estava no tesouro.** Sem país, ninguém mais consegue sacar.
- **Os Geos do banco do país**, que somem junto com ele.
- **Os 95.000 Geos** gastos com a moeda e o mercado, que não são devolvidos.

## Antes de apagar um país

Se a decisão é sua, faça isto primeiro:

1. **Retire o estoque do mercado** com `/mercado retirar`, item por item.
2. **Saque o tesouro** com `/tesouro sacar` e distribua aos membros com `/pagar`.
3. **Saque os Geos do banco** com `/pais bank withdraw`.
4. **Avise quem usa a sua moeda**, principalmente os países que a adotaram. Eles vão querer trocar por Geos ou criar a própria moeda.

## Se o país que criou a moeda que você adotou acabar

O seu país continua usando a moeda, e ela continua no seu tesouro. Mas agora ela não tem mercado nem governo por trás.

O mais seguro costuma ser trocar aos poucos por Geos e escolher outro caminho: adotar outra moeda ou [criar a própria](/wiki/pais/economia/criar-moeda/).

## Dá para recriar o país e recuperar a moeda?

Não. Um país novo, mesmo com o mesmo nome e o mesmo dono, é outro país. A moeda antiga continua congelada, e o código dela continua ocupado. O país novo precisa criar uma moeda com outro código.
