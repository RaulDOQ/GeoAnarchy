---
title: Comandos
summary: Todos os comandos de economia que um jogador pode usar, num lugar só.
order: 10
---

O que está entre `< >` é obrigatório e o que está entre `[ ]` é opcional. Não digite os sinais.

## Dinheiro

| Comando | O que faz |
|---|---|
| `/bal` | Mostra o seu dinheiro em todas as moedas |
| `/bal <jogador>` | Mostra o dinheiro de outro jogador |
| `/pay <jogador> <valor>` | Paga em Geos |
| `/pay <jogador> <valor> <moeda>` | Paga em outra moeda, por exemplo `/pay Maria 100 PESO` |
| `/baltop [moeda] [página]` | Mostra os mais ricos |

`/balance` e `/money` são o mesmo que `/bal`.

## Mercados

Os comandos de vender e comprar só funcionam dentro do ponto de venda de um mercado.

| Comando | O que faz |
|---|---|
| `/market` | Abre o menu do mercado onde você está |
| `/market list` | Lista todos os mercados, com coordenadas e preços |
| `/sell hand` | Vende a pilha que está na sua mão |
| `/sell all` | Vende tudo o que você carrega e o mercado aceita |
| `/sell <item> [quantidade]` | Vende um item. Sem a quantidade, vende todos |
| `/buy <item> [quantidade]` | Compra um item. Sem a quantidade, compra 1 |

Os nomes dos itens são em inglês, como no Minecraft: `diamond`, `iron_ingot`, `oak_log`.

## Câmbio e moedas

| Comando | O que faz |
|---|---|
| `/exchange <valor> <de> <para>` | Pede a cotação de uma troca, por exemplo `/exchange 100 GEO PESO` |
| `/exchange confirm` | Confirma a troca cotada |
| `/exchange rates` | Mostra a taxa de todas as moedas |
| `/currency list` | Lista todas as moedas |
| `/currency info <código>` | Mostra os detalhes de uma moeda |

## Menu

| Comando | O que faz |
|---|---|
| `/economy` | Abre o menu com carteira, moedas, mercados e números da economia |

## Comandos que não existem aqui

Em outros servidores você talvez use `/worth` ou venda itens de qualquer lugar. No GeoAnarchy isso não existe: o preço de um item depende do mercado, e para vender é preciso estar num deles.

Os comandos para quem governa um país estão na [Wiki do País](/wiki/pais/economia/comandos/).
