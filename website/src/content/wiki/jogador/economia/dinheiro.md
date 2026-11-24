---
title: Dinheiro e carteira
summary: O Geo, a sua carteira, como pagar outros jogadores e como escrever valores nos comandos.
order: 2
---

## O Geo

O **Geo** é a moeda do mundo. É com ele que os mercados continentais pagam e cobram, e é a moeda que os outros plugins do servidor usam.

Todo jogador começa com **0 Geos**. O seu dinheiro fica na sua carteira, não no inventário: ele não cai no chão quando você morre e ninguém pode roubá-lo.

## A sua carteira

A carteira guarda todas as moedas que você tem: Geos e moedas de países. Para ver:

```text
/bal
```

Se você só tem Geos, aparece uma linha. Se tem outras moedas, aparece a lista, com o valor aproximado de cada uma em Geos:

```text
[GeoEconomy] Your money:
 - 1,200.00 Geos
 - 5,000.00 Pesos (worth about 480.00 Geos)
```

Para ver o dinheiro de outro jogador, use `/bal Nome`. Os atalhos `/balance` e `/money` fazem a mesma coisa.

## Pagar outro jogador

```text
/pay Maria 250
```

Isso envia 250 Geos para a Maria. Para pagar em outra moeda, escreva o código dela no final:

```text
/pay Maria 100 PESO
```

Regras do pagamento:

- Não tem taxa: a Maria recebe exatamente o que você enviou.
- O jogador precisa já ter entrado no servidor alguma vez. Ele não precisa estar online.
- O valor mínimo é 0.01.
- **Não dá para desfazer.** Confira o nome e o valor antes de apertar Enter.

## Como escrever valores

| Você digita | Significa |
|---|---|
| `250` | 250 |
| `12.5` | 12 e 50 centavos |
| `1,000` | 1.000 (mil) |
| `3k` | 3.000 |
| `2.5m` | 2.500.000 |
| `1b` | 1.000.000.000 |

> **Atenção:** o jogo usa o padrão americano. O **ponto** separa os centavos e a **vírgula** separa os milhares. Se você digitar `12,5` querendo dizer "doze e cinquenta", o jogo entende **125**. Para centavos, use sempre o ponto: `12.5`.

Os valores que o jogo mostra seguem o mesmo padrão: `1,234.50 Geos` quer dizer mil duzentos e trinta e quatro Geos e cinquenta centavos.

## Como ganhar dinheiro

- **Vendendo itens num mercado.** É a forma principal. Veja [Vender e comprar](/wiki/economia/vender-e-comprar/).
- **Recebendo de outros jogadores**, por serviços, trocas ou salário do seu país.
- **Levando mercadoria de um mercado para outro.** Veja [Rotas de comércio](/wiki/economia/rotas-de-comercio/).
- **Trocando moedas** na hora certa. Veja [Câmbio](/wiki/economia/cambio/).

## Quem tem mais

```text
/baltop
```

Mostra os jogadores mais ricos em Geos. Para o ranking de outra moeda, escreva o código: `/baltop PESO`. Para ver outra página, escreva o número: `/baltop 2`.
