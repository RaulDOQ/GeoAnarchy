---
title: Comandos do país
summary: Todos os comandos para quem governa a economia de um país.
order: 13
---

Estes comandos só funcionam para o **dono do país** e para membros de um cargo com a permissão **Manage economy**. O que está entre `< >` é obrigatório e o que está entre `[ ]` é opcional.

Os comandos de jogador (pagar, vender, comprar, trocar) estão na [Wiki do Jogador](/wiki/economia/comandos/).

## Banco e tesouro

| Comando | O que faz |
|---|---|
| `/lands deposit <valor>` | Deposita Geos no banco do país |
| `/lands withdraw <valor>` | Saca Geos do banco do país |
| `/treasury` | Mostra o banco e todas as moedas do tesouro |
| `/treasury deposit <valor> <moeda>` | Deposita uma moeda no tesouro |
| `/treasury withdraw <valor> <moeda>` | Saca uma moeda do tesouro |

## Moeda própria

| Comando | O que faz | Custo |
|---|---|---|
| `/currency create <código> <nome> <plural> [símbolo]` | Cria a moeda do país | 70.000 Geos |
| `/currency print <valor>` | Imprime para o tesouro | Até 10% do total por semana |
| `/currency print` | Mostra quanto ainda pode ser impresso | |
| `/currency info <código>` | Mostra taxa, total, impressões e quem usa | |

## Adoção

| Comando | Quem usa | O que faz |
|---|---|---|
| `/currency adopt <código>` | País sem moeda | Adota a moeda de outro país, ou pede para adotar |
| `/currency abandon` | País sem moeda | Deixa de usar a moeda adotada, ou cancela o pedido |
| `/currency policy open` | Dono da moeda | Qualquer país pode adotar |
| `/currency policy approval` | Dono da moeda | Cada adoção precisa da sua aprovação |
| `/currency requests` | Dono da moeda | Lista os pedidos em espera |
| `/currency accept <país>` | Dono da moeda | Aceita um pedido |
| `/currency deny <país>` | Dono da moeda | Recusa um pedido |

## Mercado

| Comando | O que faz | Observação |
|---|---|---|
| `/market create <nome>` | Constrói o mercado onde você está | 25.000 Geos, dentro do território |
| `/market item <item> <on\|off>` | Adiciona ou tira um item da lista | Também pelo botão **Manage items** |
| `/market tax <porcentagem>` | Define o imposto | De 0 a 10 |
| `/market price <item> <porcentagem>` | Ajusta o preço de um item | De 50 a 200; 100 é o normal |
| `/market take <item> <quantidade>` | Retira itens do estoque | Estando no ponto de venda |
| `/market open` | Abre o mercado | |
| `/market close` | Fecha o mercado | O estoque fica guardado |
| `/market move` | Muda o centro para onde você está | Dentro do território |
| `/market remove confirm` | Remove o mercado | Apaga o estoque junto |

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

## Se você governa mais de um país

Os comandos valem para o país onde você está pisando. Fora de todos eles, valem para o país escolhido com `/lands edit`. Se o jogo não souber qual é, ele pede para você entrar em um deles.
