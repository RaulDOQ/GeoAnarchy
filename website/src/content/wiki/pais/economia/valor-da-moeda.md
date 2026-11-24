---
title: Valor da moeda e inflação
summary: De onde vem o valor da sua moeda, o que a fortalece, o que a derruba e como acompanhar.
order: 5
---

## De onde vem o valor

O valor da sua moeda é a **taxa de câmbio** dela contra o Geo. Ninguém define essa taxa: ela sai da **reserva de câmbio** da moeda.

A reserva guarda duas coisas: Geos e unidades da sua moeda. A taxa é a proporção entre elas.

| Na reserva | Taxa |
|---|---|
| 20.000 Geos e 200.000 Pesos | 1 Geo = 10 Pesos |
| 25.000 Geos e 160.000 Pesos | 1 Geo = 6,4 Pesos (o Peso valorizou) |
| 16.000 Geos e 250.000 Pesos | 1 Geo = 15,6 Pesos (o Peso desvalorizou) |

- Quando um jogador **compra** a sua moeda, entra Geo e sai moeda: ela **valoriza**.
- Quando um jogador **vende** a sua moeda, entra moeda e sai Geo: ela **desvaloriza**.

A reserva nunca esvazia. Quanto mais gente tira de um lado, mais caro aquele lado fica.

## A reserva não é do país

Os 20.000 Geos que você colocou ao criar a moeda ficam na reserva **para sempre**. O país não pode sacá-los.

É isso que dá segurança a quem aceita a sua moeda: sempre haverá Geos do outro lado para trocar.

## O que fortalece a moeda

- **Um mercado movimentado.** Para comprar no seu mercado, os jogadores precisam da sua moeda. Eles compram no câmbio e ela valoriza.
- **Itens que só o seu mercado tem**, ou que ele vende mais barato.
- **Outros países adotando a moeda.** Mais gente usando, mais procura.
- **Imprimir pouco.**

## O que derruba a moeda

- **Imprimir muito e gastar rápido.**
- **Um mercado que só compra.** Os vendedores recebem a sua moeda e, sem ter o que comprar com ela, trocam tudo por Geos.
- **Perda de confiança.** Se os jogadores acham que a moeda vai cair, eles vendem, e ela cai.

## O que é inflação aqui

Os preços do seu mercado partem dos preços em Geos, convertidos pela taxa de câmbio. Se a sua moeda perde valor, **tudo no seu mercado fica mais caro na sua moeda**.

> **Exemplo:** um lingote de ferro vale 10 Geos. Com o Peso a 10 por Geo, ele custa 100 Pesos. Se o Peso cair para 15 por Geo, o mesmo lingote passa a custar 150 Pesos. Isso é uma inflação de 50%.

Quem perde com a inflação:

- quem guardava a moeda (inclusive o tesouro do país);
- os países que adotaram a moeda;
- o próprio país, porque os jogadores passam a evitar a moeda.

## Os preços do mercado reagem com atraso

O mercado do país não usa a taxa do segundo: usa uma taxa **suavizada**, que acompanha a real com cerca de 30 minutos de atraso.

Isso protege o país. Sem o atraso, alguém poderia mexer na taxa por um instante com uma troca grande e comprar ou vender no seu mercado por um preço falso.

Na prática: depois de uma mudança forte no câmbio, os preços do mercado levam cerca de meia hora para se ajustar.

## Como acompanhar

```text
/currency info PESO
```

Mostra a taxa atual, o total da moeda, quanto foi impresso na semana e quem usa a moeda.

No menu `/economy`:

- **Currencies** mostra a variação do valor em 24 horas e em 7 dias, e quanto há na reserva.
- **Economy** mostra a **inflação** de cada moeda em 24 horas e em 7 dias.

> **Dica:** olhe a inflação da sua moeda antes de imprimir. Se ela já está subindo, imprimir mais vai piorar.
