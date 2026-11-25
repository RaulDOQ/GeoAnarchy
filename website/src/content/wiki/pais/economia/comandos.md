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
| `/tesouro depositar <valor> <moeda>` | Deposita uma moeda no tesouro |
| `/tesouro sacar <valor> <moeda>` | Saca uma moeda do tesouro |

## Moeda própria

| Comando | O que faz | Custo |
|---|---|---|
| `/moeda criar <código> <nome> <plural> [símbolo]` | Cria a moeda do país | 70.000 Geos |
| `/moeda imprimir <valor>` | Imprime para o tesouro | Até 10% do total por semana |
| `/moeda imprimir` | Mostra quanto ainda pode ser impresso | |
| `/moeda info <código>` | Mostra taxa, total, impressões e quem usa | |

## Adoção

| Comando | Quem usa | O que faz |
|---|---|---|
| `/moeda adotar <código>` | País sem moeda | Adota a moeda de outro país, ou pede para adotar |
| `/moeda abandonar` | País sem moeda | Deixa de usar a moeda adotada, ou cancela o pedido |
| `/moeda politica aberta` | Dono da moeda | Qualquer país pode adotar |
| `/moeda politica aprovacao` | Dono da moeda | Cada adoção precisa da sua aprovação |
| `/moeda pedidos` | Dono da moeda | Lista os pedidos em espera |
| `/moeda aceitar <país>` | Dono da moeda | Aceita um pedido |
| `/moeda recusar <país>` | Dono da moeda | Recusa um pedido |

## Mercado

| Comando | O que faz | Observação |
|---|---|---|
| `/mercado criar <nome>` | Constrói o mercado onde você está | 25.000 Geos, dentro do território |
| `/mercado item <item> <ligar\|desligar>` | Adiciona ou tira um item da lista | Também pelo botão **Gerenciar itens** |
| `/mercado imposto <porcentagem>` | Define o imposto | De 0 a 10 |
| `/mercado preco <item> <porcentagem>` | Ajusta o preço de um item | De 50 a 200; 100 é o normal |
| `/mercado retirar <item> <quantidade>` | Retira itens do estoque | Estando no ponto de venda |
| `/mercado abrir` | Abre o mercado | |
| `/mercado fechar` | Fecha o mercado | O estoque fica guardado |
| `/mercado mover` | Muda o centro para onde você está | Dentro do território |
| `/mercado remover confirmar` | Remove o mercado | Apaga o estoque junto |

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

Os comandos também funcionam em inglês, e as palavras depois deles também: `/treasury deposit 5000 PESO` é o mesmo que `/tesouro depositar 5000 PESO`, e `/market tax 5` é o mesmo que `/mercado imposto 5`. `/currency` é o `/moeda` e `/lands` é o `/pais`.

Os comandos do **/pais** são do plugin Lands e as palavras depois dele continuam em inglês: `/pais claim`, `/pais bank deposit`, `/pais trust`.

## Se você governa mais de um país

Os comandos valem para o país onde você está pisando. Fora de todos eles, valem para o país escolhido com `/pais edit`. Se o jogo não souber qual é, ele pede para você entrar em um deles.
