---
title: Comandos
summary: Todos os comandos de economia que um jogador pode usar, num lugar só.
order: 10
---

O que está entre `< >` é obrigatório e o que está entre `[ ]` é opcional. Não digite os sinais.

## Dinheiro

| Comando | O que faz |
|---|---|
| `/saldo` | Mostra o seu dinheiro em todas as moedas |
| `/saldo <jogador>` | Mostra o dinheiro de outro jogador |
| `/pagar <jogador> <valor>` | Paga em Geos |
| `/pagar <jogador> <valor> <moeda>` | Paga em outra moeda, por exemplo `/pagar Maria 100 PESO` |
| `/ricos [moeda] [página]` | Mostra os mais ricos |

`/dinheiro` é o mesmo que `/saldo`.

## Mercados

Os comandos de vender e comprar só funcionam dentro do ponto de venda de um mercado.

| Comando | O que faz |
|---|---|
| `/mercado` | Abre o menu do mercado onde você está |
| `/mercado lista` | Lista todos os mercados, com coordenadas e preços |
| `/vender mao` | Vende a pilha que está na sua mão |
| `/vender tudo` | Vende tudo o que você carrega e o mercado aceita |
| `/vender <item> [quantidade]` | Vende um item. Sem a quantidade, vende todos |
| `/comprar <item> [quantidade]` | Compra um item. Sem a quantidade, compra 1 |

Os nomes dos itens são em inglês, como no Minecraft: `diamond`, `iron_ingot`, `oak_log`.

## Câmbio e moedas

| Comando | O que faz |
|---|---|
| `/cambio <valor> <de> <para>` | Pede a cotação de uma troca, por exemplo `/cambio 100 GEO PESO` |
| `/cambio confirmar` | Confirma a troca cotada |
| `/cambio taxas` | Mostra a taxa de todas as moedas |
| `/moeda lista` | Lista todas as moedas |
| `/moeda info <código>` | Mostra os detalhes de uma moeda |

## Menu

| Comando | O que faz |
|---|---|
| `/economia` | Abre o menu com carteira, moedas, mercados e números da economia |

## Nomes em inglês

Cada comando também funciona com o nome em inglês, que é o mesmo de outros servidores:

| Comando | Em inglês |
|---|---|
| `/saldo` | `/bal`, `/balance`, `/money` |
| `/pagar` | `/pay` |
| `/ricos` | `/baltop` |
| `/mercado` | `/market` |
| `/vender` | `/sell` |
| `/comprar` | `/buy` |
| `/cambio` | `/exchange` |
| `/moeda` | `/currency` |
| `/economia` | `/economy` |

As palavras depois do comando também funcionam em inglês: `/sell all` é o mesmo que `/vender tudo`, e `/exchange confirm` é o mesmo que `/cambio confirmar`. Acentos não fazem diferença: `/vender mão` e `/vender mao` funcionam.

Os valores aceitam vírgula: `/pagar Maria 12,5` envia 12 Geos e 50 centavos.

## Comandos que não existem aqui

Em outros servidores você talvez use `/worth` ou venda itens de qualquer lugar. No GeoAnarchy isso não existe: o preço de um item depende do mercado, e para vender é preciso estar num deles.

Os comandos para quem governa um país estão na [Wiki do País](/wiki/pais/economia/comandos/).
