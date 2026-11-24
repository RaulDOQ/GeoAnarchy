---
title: Câmbio
summary: Como trocar uma moeda por outra, quanto custa e por que a taxa muda.
order: 8
---

O câmbio funciona de **qualquer lugar**: você não precisa estar num mercado.

## Como trocar

1. Peça uma cotação. Escreva o valor, a moeda que você tem e a moeda que você quer:

   ```text
   /exchange 100 GEO PESO
   ```

2. O jogo mostra quanto você vai receber:

   ```text
   Trocar 100.00 Geos por 985.07 Pesos (taxa 1.00 Geo, 1 Geo = 9.85 Pesos)
   [Confirmar] ou digite /exchange confirm em até 30 segundos.
   ```

3. Clique em **[Confirmar]** ou digite `/exchange confirm`. Você tem 30 segundos.

Nada acontece enquanto você não confirmar. Se a cotação não agradar, é só não confirmar.

Para ver as taxas de todas as moedas de uma vez:

```text
/exchange rates
```

## A taxa de 1%

Toda troca paga **1% de taxa**, descontada do valor que você entrega. No exemplo acima, 1 dos 100 Geos é a taxa.

Por causa da taxa, trocar e destrocar em seguida sempre dá prejuízo.

## De onde vem a taxa de câmbio

Cada moeda de país tem uma **reserva** com duas coisas: Geos e a própria moeda. A taxa é a proporção entre as duas.

- Quando alguém **compra** Pesos, tira Pesos da reserva e coloca Geos. Sobram menos Pesos: o Peso fica **mais caro**.
- Quando alguém **vende** Pesos, acontece o contrário: o Peso fica **mais barato**.

Ninguém define a taxa na mão. Ela é o resultado de todas as trocas que os jogadores fizeram.

## Trocas grandes saem mais caras

A sua própria troca move a taxa. Quanto maior o valor, pior a cotação que você recebe.

| Você troca | Recebe | Cada Geo rendeu |
|---|---|---|
| 100 Geos | 985 Pesos | 9,85 Pesos |
| 1.000 Geos | 9.433 Pesos | 9,43 Pesos |
| 5.000 Geos | 39.679 Pesos | 7,94 Pesos |

(Valores para uma moeda recém-criada, com a taxa inicial de 10 Pesos por Geo.)

A cotação que o jogo mostra já é o valor exato para a quantidade que você pediu. Sempre confira antes de confirmar.

## Se a taxa mudar antes de você confirmar

Se outro jogador fizer uma troca entre a sua cotação e a sua confirmação, a taxa pode mudar. Se a diferença for pequena, a troca acontece normalmente. Se você fosse receber bem menos do que foi mostrado, o jogo **cancela** e mostra uma cotação nova, com a mensagem `The rate moved before you confirmed`. Você decide de novo.

## Trocar entre duas moedas de países

Funciona com um comando só:

```text
/exchange 500 PESO FRANCO
```

Por baixo, a troca passa pelo Geo: os Pesos viram Geos e os Geos viram Francos. A taxa de 1% é cobrada uma vez.

## O que as mensagens querem dizer

| Mensagem | O que aconteceu |
|---|---|
| `Você não tem o suficiente` | Você não tem aquele valor na moeda que quer entregar |
| `Esse valor é pequeno demais para trocar` | O valor é pequeno demais para render pelo menos um centavo |
| `Essas são a mesma moeda` | Você escreveu a mesma moeda duas vezes |
| `Não existe moeda com o código ...` | O código não existe. Confira em `/currency list` |
| `Nada para confirmar` | Passaram os 30 segundos, ou você já confirmou. Peça a cotação de novo |
| `Você não pode guardar tanto` | Você passaria do limite de saldo |

## Dá para lucrar com câmbio?

Trocar e destrocar na hora, não: a taxa garante o prejuízo.

O que existe é apostar no futuro de uma moeda. Comprar a moeda de um país que está crescendo e vender quando ela valorizar pode dar lucro. Também pode dar errado, se o país resolver imprimir dinheiro. Veja [Moedas dos países](/wiki/economia/moedas/).
