---
title: Gerenciar o mercado
summary: Imposto, preços por item, estoque, abrir e fechar, mudar de lugar e manter o tesouro saudável.
order: 9
---

Os comandos desta página valem para o mercado do país que você governa. Só `/mercado take` exige que você esteja no ponto de venda.

## Imposto

```text
/mercado tax 5
```

O imposto vai de **0 a 10%** e vale para toda negociação:

- quem **vende** recebe um pouco menos;
- quem **compra** paga um pouco mais.

A diferença fica com o tesouro. Com `/mercado tax 0` não há imposto.

> **Cuidado:** imposto alto espanta os comerciantes. Se o mercado vizinho cobra menos, é para lá que eles vão.

## Preço por item

```text
/mercado price wheat 150
```

Faz o mercado negociar aquele item a uma porcentagem do preço normal. O valor vai de **50 a 200**.

| Valor | Efeito | Quando usar |
|---|---|---|
| `150` | Paga e cobra 50% a mais | O país **precisa** do item e quer atrair vendedores |
| `100` | Preço normal | Para voltar ao padrão |
| `70` | Paga e cobra 30% a menos | O país tem demais, ou quer proteger o tesouro |

O ajuste vale para a compra e para a venda ao mesmo tempo.

> **Exemplo:** a Espanha está construindo muralhas e define `/mercado price obsidian 180`. Jogadores do servidor inteiro passam a levar obsidiana para Madri, porque lá rende quase o dobro.

## Retirar itens do estoque

Tudo o que os jogadores vendem ao mercado fica no estoque, e o estoque é do país. Para pegar itens para uso do país:

```text
/mercado take obsidian 64
```

Você precisa estar **no ponto de venda** e ter espaço no inventário.

Retirar itens diminui o estoque. Com menos estoque, o preço daquele item no mercado sobe.

É assim que um país usa o mercado para se abastecer: paga caro por um item, deixa os jogadores trazerem, e retira.

## Abrir e fechar

```text
/mercado close
/mercado open
```

Com o mercado fechado, ninguém vende nem compra. O estoque e as configurações ficam guardados. Útil durante uma guerra, ou enquanto você reorganiza a lista de itens.

## Mudar de lugar

Vá até o novo ponto, **dentro do território do país**, e digite:

```text
/mercado move
```

O centro do mercado passa a ser onde você está. Não custa nada, e o estoque vai junto.

## Remover o mercado

```text
/mercado remove confirm
```

> **Atenção:** remover apaga o mercado **e todo o estoque dele**. Os 25.000 Geos da construção não voltam. Retire antes os itens que quiser guardar.

## Manter o tesouro saudável

O mercado paga os vendedores com o tesouro. Se ele zerar, o mercado para de comprar.

**Sinais de problema:**

- o saldo em `/tesouro` só cai;
- o mercado compra muito mais do que vende.

**O que fazer:**

- **Baixe o preço** dos itens que mais entram, com `/mercado price`.
- **Tire da lista** os itens que estão drenando o tesouro.
- **Aumente o imposto** um pouco.
- **Venda o estoque.** Um mercado com estoque cheio e preço baixo atrai compradores, e eles pagam ao tesouro.
- **Deposite** no tesouro com `/tesouro deposit`.
- **Imprima**, em último caso. Veja o custo em [Imprimir dinheiro](/wiki/pais/economia/imprimir-dinheiro/).

## O estoque do país não some

Nos mercados continentais, o estoque em excesso desaparece aos poucos. No mercado de um país isso **não acontece**: o que entra fica até alguém comprar ou o país retirar.

O lado ruim: um item encalhado continua barato até alguém dar um destino a ele.

## Resumo dos comandos

| Comando | O que faz |
|---|---|
| `/mercado tax <0 a 10>` | Define o imposto |
| `/mercado price <item> <50 a 200>` | Ajusta o preço de um item |
| `/mercado item <item> <on\|off>` | Adiciona ou tira um item da lista |
| `/mercado take <item> <quantidade>` | Retira itens do estoque |
| `/mercado open` e `/mercado close` | Abre e fecha o mercado |
| `/mercado move` | Muda o centro para onde você está |
| `/mercado remove confirm` | Remove o mercado e o estoque |
