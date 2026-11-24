---
title: O menu /economy
summary: Um menu com a sua carteira, todas as moedas, todos os mercados e os números da economia do servidor.
order: 9
---

```text
/economy
```

Abre um menu com quatro páginas. `/economia` faz o mesmo. Ele funciona de qualquer lugar e é só para consulta: nada ali gasta o seu dinheiro.

## Minha carteira

- Um ícone para cada moeda que você tem, com o saldo.
- Para moedas de países: quanto aquele saldo vale em Geos e qual país emitiu a moeda.
- O livro **Transações recentes** mostra as suas últimas movimentações: vendas, compras, pagamentos e trocas. Verde é dinheiro que entrou, vermelho é dinheiro que saiu.

## Moedas

Um ícone para cada moeda do servidor. Passe o mouse para ver:

- a taxa de câmbio de agora;
- **Valor: 24h / 7d**: quanto a moeda valorizou ou desvalorizou em 24 horas e em 7 dias. Verde é alta, vermelho é queda;
- o total que existe (**Total em circulação**) e quanto foi impresso na última semana;
- quanto há na reserva de câmbio da moeda;
- quais países usam a moeda e se a adoção é livre ou precisa de aprovação;
- as últimas impressões, com data, valor e quem imprimiu.

## Mercados

A mesma lista do `/market list`: todos os mercados, do mais perto para o mais longe, com coordenadas, distância, moeda e nível de preços.

Clique num mercado para ver a lista de preços dele. De longe você só consegue olhar. Para negociar, é preciso estar lá.

## Economia

Os números do servidor inteiro:

- **Geos em circulação**: quantos Geos existem nas carteiras e nas reservas de câmbio, e quanto isso mudou em 24 horas e em 7 dias.
- **Dinheiro criado e removido**: quantos Geos os mercados continentais pagaram e quantos receberam de volta nas últimas 24 horas.
- **Itens que mais renderam**: os itens que mais renderam Geos para os jogadores nas últimas 24 horas. Bom para saber o que está valendo a pena vender.
- **Níveis de preços**: o nível de preços de cada mercado.
- Um ícone por moeda de país, com a **inflação** dela em 24 horas e em 7 dias.

## Como ler os números

| No jogo | Quer dizer |
|---|---|
| `Nível de preços: 100% do normal` | Os preços daquele mercado estão no normal |
| `Nível de preços` abaixo de 100% | O mercado está bem abastecido: bom para comprar |
| `Nível de preços` acima de 100% | Faltam itens: bom para vender |
| `Inflação` em vermelho, com `+` | As coisas ficaram mais caras naquela moeda |
| `sem dados ainda` | O servidor ainda não tem histórico suficiente para comparar |

Os números de 24 horas e de 7 dias só aparecem depois que o servidor ficou ligado por esse tempo.
