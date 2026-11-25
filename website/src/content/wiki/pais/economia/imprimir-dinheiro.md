---
title: Imprimir dinheiro
summary: Como emitir mais da sua moeda, qual é o limite e o preço que o país paga por exagerar.
order: 4
---

## O comando

```text
/moeda print 30000
```

O dinheiro novo vai direto para o **tesouro do país**. Só o país que criou a moeda pode imprimir. Um país que apenas adotou a moeda não pode.

Para ver quanto ainda dá para imprimir, digite só:

```text
/moeda print
```

## O limite: 10% por semana

O país pode imprimir, a cada 7 dias, até **10% do total da moeda** que existia antes das impressões da semana.

> **Exemplo:** a moeda acabou de ser criada e tem 300.000 unidades. Nesta semana o país pode imprimir até 30.000. Depois disso o total é 330.000, e quando essas impressões completarem 7 dias o limite passa a ser 33.000.

O limite conta os últimos 7 dias corridos, não a semana do calendário. Se você imprimiu tudo numa segunda-feira, só volta a poder na segunda seguinte.

## Todo mundo fica sabendo

Cada impressão é anunciada no chat para o **servidor inteiro**, com o valor impresso e o novo total da moeda. Ela também fica registrada em `/moeda info` e no menu `/economia`, com a data e o nome de quem imprimiu.

Não dá para imprimir escondido. Os jogadores que guardam a sua moeda vão ver.

## O preço de imprimir

Imprimir não custa Geos. O custo é outro: **cada unidade nova faz as outras valerem um pouco menos.**

O dinheiro impresso entra na economia quando o país gasta: pagando pessoas, ou pagando quem vende no mercado. Quando esse dinheiro chega ao câmbio, a moeda perde valor.

> **Exemplo:** o Peso vale 0,10 Geo (10 Pesos por Geo). A Espanha imprime 30.000 Pesos e troca tudo por Geos de uma vez.
>
> - Ela recebe cerca de **2.586 Geos**, e não os 3.000 que a taxa antiga sugeria.
> - A taxa passa de 10 para cerca de **13,2 Pesos por Geo**.
> - Tudo o que é vendido em Pesos fica cerca de 32% mais caro.
> - Quem guardava Pesos, inclusive o tesouro e os países que adotaram a moeda, perdeu cerca de um quarto do valor.

Quanto mais se imprime, menos cada impressão rende. É a mesma conta do mundo real.

## Quando imprimir faz sentido

- **A economia do país cresceu.** Mais gente usa a moeda, o mercado movimenta mais, e falta dinheiro em circulação. Imprimir acompanha o crescimento sem derrubar o valor.
- **O tesouro precisa pagar o mercado.** Se o mercado compra muito, o tesouro esvazia. Imprimir um pouco mantém o mercado funcionando.
- **Uma emergência.** Guerra, reconstrução. Funciona, mas o país paga com inflação depois.

## Quando é má ideia

- **Imprimir o máximo toda semana.** A moeda perde valor sem parar, e os jogadores deixam de aceitá-la.
- **Imprimir para trocar por Geos.** Cada rodada rende menos que a anterior e destrói a confiança na moeda.
- **Imprimir sem ter um mercado.** Sem lugar para gastar a moeda, todo o dinheiro novo vai parar no câmbio.

> **Dica:** um país conhecido por imprimir pouco tem uma moeda que as pessoas querem guardar. Isso vale mais do que qualquer impressão.

## Se o comando for recusado

| Mensagem | Motivo |
|---|---|
| `Isso passa do limite desta semana` | O valor passa do que ainda pode ser impresso. A mensagem diz quanto resta |
| `não tem moeda própria` | O país não criou uma moeda (ou só adotou a de outro) |
| `Você não comanda a economia de nenhum país` | Você não é o dono nem tem a permissão |
