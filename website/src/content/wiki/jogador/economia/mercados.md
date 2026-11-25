---
title: Mercados
summary: O que é um mercado, os dois tipos que existem, como achar um e o que cada um aceita.
order: 3
---

## O que é um mercado

Um mercado é um **lugar no mapa** onde você vende e compra itens. Não existe um comando para vender de qualquer lugar: você precisa ir até lá.

Cada mercado tem três coisas só dele:

- **Um estoque.** Tudo o que os jogadores venderam ali fica guardado ali, e é só isso que o mercado tem para vender.
- **Uma lista de itens.** O mercado só negocia os itens que o dono dele escolheu.
- **Preços próprios**, que dependem do estoque daquele mercado.

## Os dois tipos

| | Mercado continental | Mercado de país |
|---|---|---|
| **Quem criou** | A administração do servidor | Um país, dentro do próprio território |
| **Moeda** | Geo | A moeda daquele país |
| **Quem paga quando você vende** | O próprio mercado | O tesouro do país |
| **Pode ficar sem dinheiro?** | Não | Sim. Se o tesouro esvaziar, ele para de comprar |
| **Imposto** | Normalmente nenhum | O país pode cobrar até 10% |

Para vender num mercado de país você recebe na moeda dele. Para comprar, precisa ter a moeda dele. Você consegue essa moeda no [câmbio](/wiki/economia/cambio/).

## Como achar um mercado

```text
/mercado list
```

Abre um menu com todos os mercados, do mais perto para o mais longe. Passe o mouse em cima de um mercado para ver:

- se é continental ou de qual país é;
- quantos itens ele negocia e em qual moeda;
- as **coordenadas** e a distância em blocos;
- o nível de preços (100% é o normal).

Clique num mercado para ver a lista de preços dele, mesmo estando longe. Assim você decide se a viagem vale a pena antes de sair.

## O ponto de venda

Cada mercado tem um **ponto de venda**: uma área de cerca de 10 blocos em volta do centro. Você sabe que entrou quando:

- aparece um aviso acima da sua barra de itens, com o nome do mercado;
- você vê um **anel de partículas** marcando a borda.

Só dentro dessa área os comandos `/mercado`, `/vender` e `/comprar` funcionam. Fora dela, o jogo avisa que você não está num mercado.

> **Atenção:** o servidor não protege o ponto de venda. Outros jogadores podem estar esperando por você lá. Chegue com cuidado, principalmente se estiver carregando muita coisa.

## O que um mercado aceita

**Só itens da lista dele.** Um mercado novo começa sem nenhum item, e o dono vai adicionando um por um. Por isso mercados diferentes negociam coisas diferentes. Se um mercado não negocia o que você trouxe, o jeito é vender em outro.

**Só itens "limpos".** O mercado recusa:

- itens danificados (uma picareta usada, por exemplo);
- itens renomeados na bigorna;
- itens encantados;
- caixas de shulker.

**Só enquanto houver espaço.** Quando o mercado junta muito de um item, ele para de comprar aquele item por um tempo. Veja [Como os preços funcionam](/wiki/economia/precos/).

## O estoque começa vazio

Todo mercado novo começa com o estoque zerado. Se ninguém vendeu diamantes na Europa, não há diamantes para comprar na Europa. Alguém precisa levar.

Isso é o que faz o comércio entre regiões existir: o que sobra num lugar falta em outro.
