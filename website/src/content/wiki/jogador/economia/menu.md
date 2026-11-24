---
title: O menu /economy
summary: Um menu com a sua carteira, todas as moedas, todos os mercados e os números da economia do servidor.
order: 9
---

```text
/economy
```

Abre um menu com quatro páginas. Ele funciona de qualquer lugar e é só para consulta: nada ali gasta o seu dinheiro.

## My wallet (minha carteira)

- Um ícone para cada moeda que você tem, com o saldo.
- Para moedas de países: quanto aquele saldo vale em Geos e qual país emitiu a moeda.
- O livro **Recent transactions** mostra as suas últimas movimentações: vendas, compras, pagamentos e trocas. Verde é dinheiro que entrou, vermelho é dinheiro que saiu.

## Currencies (moedas)

Um ícone para cada moeda do servidor. Passe o mouse para ver:

- a taxa de câmbio de agora;
- **Value: 24h / 7d**: quanto a moeda valorizou ou desvalorizou em 24 horas e em 7 dias. Verde é alta, vermelho é queda;
- o total que existe (**Total supply**) e quanto foi impresso na última semana;
- quanto há na reserva de câmbio da moeda;
- quais países usam a moeda e se a adoção é livre ou precisa de aprovação;
- as últimas impressões, com data, valor e quem imprimiu.

## Markets (mercados)

A mesma lista do `/market list`: todos os mercados, do mais perto para o mais longe, com coordenadas, distância, moeda e nível de preços.

Clique num mercado para ver a lista de preços dele. De longe você só consegue olhar. Para negociar, é preciso estar lá.

## Economy (economia)

Os números do servidor inteiro:

- **Geos in circulation**: quantos Geos existem nas carteiras e nas reservas de câmbio, e quanto isso mudou em 24 horas e em 7 dias.
- **New and removed money**: quantos Geos os mercados continentais pagaram e quantos receberam de volta nas últimas 24 horas.
- **Items that brought in the most**: os itens que mais renderam Geos para os jogadores nas últimas 24 horas. Bom para saber o que está valendo a pena vender.
- **Price levels**: o nível de preços de cada mercado.
- Um ícone por moeda de país, com a **inflação** dela em 24 horas e em 7 dias.

## Como ler os números

| No jogo | Quer dizer |
|---|---|
| `Price level: 100% of normal` | Os preços daquele mercado estão no normal |
| `Price level` abaixo de 100% | O mercado está bem abastecido: bom para comprar |
| `Price level` acima de 100% | Faltam itens: bom para vender |
| `Inflation` em vermelho, com `+` | As coisas ficaram mais caras naquela moeda |
| `no data yet` | O servidor ainda não tem histórico suficiente para comparar |

Os números de 24 horas e de 7 dias só aparecem depois que o servidor ficou ligado por esse tempo.
