---
title: Moedas dos países
summary: Além do Geo, cada país pode ter a própria moeda. O que elas valem e o que acontece quando um país imprime dinheiro.
order: 7
---

## O que são

Um país pode criar a própria moeda: o Peso, o Franco, o que ele quiser. Ela tem nome, um **código** de 3 a 5 letras (como `PESO`) e um valor que muda com o tempo.

Só o país que criou a moeda pode **imprimir** mais dela. Outros países podem **adotar** a moeda, isto é, usá-la como moeda oficial sem controlar nada.

O Geo continua existindo ao lado de todas elas. Ele é a moeda do mundo e é a referência de valor: toda moeda de país vale "tantos Geos".

## Como você recebe moedas de países

- **Vendendo no mercado de um país.** Ele paga na moeda dele.
- **Recebendo de outro jogador**, com `/pagar Nome 100 PESO`.
- **Trocando no câmbio**, com `/cambio`. Veja [Câmbio](/wiki/economia/cambio/).

Todas ficam na mesma carteira. O `/saldo` mostra cada uma e quanto ela vale em Geos.

## Ver as moedas que existem

```text
/moeda lista
```

Mostra todas as moedas, quantas unidades 1 Geo compra e qual país emitiu cada uma.

```text
/moeda info PESO
```

Mostra os detalhes de uma moeda:

| No jogo | Quer dizer |
|---|---|
| `Emitida por Espanha` | O país que criou e controla a moeda |
| `1 Geo = 10.32 Pesos` | A taxa de câmbio de agora |
| `Total: 300,000.00 Pesos` | Quanto dessa moeda existe no total |
| `impresso nos últimos 7 dias` | Quanto o país imprimiu na última semana |
| `Ainda pode ser impresso nesta semana` | Quanto o país ainda pode imprimir nesta semana |
| `Usada por: Espanha, Argentina` | Os países que usam a moeda |

O menu `/economia` mostra as mesmas informações, com a variação das últimas 24 horas e dos últimos 7 dias.

## O valor muda

O valor de uma moeda vem do câmbio: quanto mais gente quer a moeda, mais ela vale; quanto mais gente se desfaz dela, menos vale.

Uma moeda tende a **valorizar** quando:

- o mercado do país é movimentado e as pessoas precisam dela para comprar lá;
- mais países adotam a moeda;
- o país imprime pouco.

Uma moeda tende a **desvalorizar** quando:

- o país imprime muito e despeja no câmbio;
- as pessoas param de usar o mercado do país;
- muita gente troca a moeda por Geos ao mesmo tempo.

## Quando um país imprime dinheiro

Um país pode imprimir até **10% do total da moeda por semana**. Toda impressão é anunciada para o servidor inteiro, com o valor impresso e o novo total.

Dinheiro novo não cria riqueza nova. Se o país gasta esse dinheiro e ele acaba no câmbio, cada unidade passa a valer menos. Isso é **inflação**: os preços naquela moeda sobem e as economias de quem guardava a moeda perdem valor.

> **Exemplo:** o Peso vale 0,10 Geo. A Espanha imprime 30.000 Pesos para pagar soldados, e eles trocam tudo por Geos. O Peso cai para cerca de 0,076 Geo. Quem tinha 10.000 Pesos guardados tinha o equivalente a 1.000 Geos e passou a ter 760.

Antes de guardar muito dinheiro na moeda de um país, veja em `/moeda info` quanto ele anda imprimindo.

## Se o país deixar de existir

Quando um país é apagado, a moeda dele **continua existindo**:

- o dinheiro continua na carteira de quem tinha;
- ainda dá para trocar no câmbio e pagar outros jogadores com ela;
- **ninguém mais pode imprimir**;
- os mercados daquele país são removidos.

No jogo, uma moeda assim aparece com o aviso `Its country no longer exists`.

## Vale a pena guardar moeda de país?

Depende do país. Uma moeda de um país estável, com mercado ativo, pode valorizar. Uma moeda de um país em guerra, que imprime sem parar, pode derreter.

Se você não quer correr esse risco, troque por Geos. O Geo não é impresso por nenhum país.
