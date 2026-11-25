---
title: Comandos do país
summary: Todos os comandos para quem governa a economia de um país.
order: 13
---

Estes comandos só funcionam para o **dono do país** e para membros de um cargo com a permissão **Gerenciar economia**. O que está entre `< >` é obrigatório e o que está entre `[ ]` é opcional.

Os comandos de jogador (pagar, vender, comprar, trocar) estão na [Wiki do Jogador](/wiki/economia/comandos/).

## Banco e tesouro

| Comando | O que faz |
|---|---|
| `/pais bank deposit <valor>` | Deposita Geos no banco do país |
| `/pais bank withdraw <valor>` | Saca Geos do banco do país |
| `/tesouro` | Mostra o banco e todas as moedas do tesouro |
| `/tesouro deposit <valor> <moeda>` | Deposita uma moeda no tesouro |
| `/tesouro withdraw <valor> <moeda>` | Saca uma moeda do tesouro |

## Moeda própria

| Comando | O que faz | Custo |
|---|---|---|
| `/moeda create <código> <nome> <plural> [símbolo]` | Cria a moeda do país | 70.000 Geos |
| `/moeda print <valor>` | Imprime para o tesouro | Até 10% do total por semana |
| `/moeda print` | Mostra quanto ainda pode ser impresso | |
| `/moeda info <código>` | Mostra taxa, total, impressões e quem usa | |

## Adoção

| Comando | Quem usa | O que faz |
|---|---|---|
| `/moeda adopt <código>` | País sem moeda | Adota a moeda de outro país, ou pede para adotar |
| `/moeda abandon` | País sem moeda | Deixa de usar a moeda adotada, ou cancela o pedido |
| `/moeda policy open` | Dono da moeda | Qualquer país pode adotar |
| `/moeda policy approval` | Dono da moeda | Cada adoção precisa da sua aprovação |
| `/moeda requests` | Dono da moeda | Lista os pedidos em espera |
| `/moeda accept <país>` | Dono da moeda | Aceita um pedido |
| `/moeda deny <país>` | Dono da moeda | Recusa um pedido |

## Mercado

| Comando | O que faz | Observação |
|---|---|---|
| `/mercado create <nome>` | Constrói o mercado onde você está | 25.000 Geos, dentro do território |
| `/mercado item <item> <on\|off>` | Adiciona ou tira um item da lista | Também pelo botão **Gerenciar itens** |
| `/mercado tax <porcentagem>` | Define o imposto | De 0 a 10 |
| `/mercado price <item> <porcentagem>` | Ajusta o preço de um item | De 50 a 200; 100 é o normal |
| `/mercado take <item> <quantidade>` | Retira itens do estoque | Estando no ponto de venda |
| `/mercado open` | Abre o mercado | |
| `/mercado close` | Fecha o mercado | O estoque fica guardado |
| `/mercado move` | Muda o centro para onde você está | Dentro do território |
| `/mercado remove confirm` | Remove o mercado | Apaga o estoque junto |

## Custos e limites

| | Valor |
|---|---|
| Criar a moeda | 70.000 Geos (50.000 de taxa e 20.000 para a reserva de câmbio) |
| Moeda que nasce | 300.000 unidades (200.000 na reserva e 100.000 no tesouro) |
| Taxa inicial | 1 Geo = 10 unidades |
| Impressão | Até 10% do total a cada 7 dias |
| Criar o mercado | 25.000 Geos |
| Mercados por país | 1 |
| Imposto do mercado | De 0 a 10% |
| Ajuste de preço por item | De 50% a 200% |
| Distância entre mercados | Pelo menos 25 blocos |
| Taxa de câmbio | 1% por troca |
| Fundar o país | 20.000 Geos |
| Cada chunk a mais | 1.000 Geos |
| Manutenção | 200 Geos por chunk, todo dia às 15:00 |

## Nomes em inglês

Os comandos também funcionam em inglês: `/treasury` é o `/tesouro`, `/currency` é o `/moeda`, `/market` é o `/mercado` e `/lands` é o `/pais`. O que vem depois é igual nos dois: `/tesouro deposit 5000 PESO` e `/treasury deposit 5000 PESO` fazem a mesma coisa.

## Se você governa mais de um país

Os comandos valem para o país onde você está pisando. Fora de todos eles, valem para o país escolhido com `/pais edit`. Se o jogo não souber qual é, ele pede para você entrar em um deles.
