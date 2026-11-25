---
title: Banco e tesouro
summary: Onde fica o dinheiro do país, como depositar e sacar, e para que ele é usado.
order: 2
---

## Dois lugares, um tesouro

O dinheiro de um país fica em dois lugares:

| | O que guarda | Como mexer |
|---|---|---|
| **Banco do país** | Os **Geos** do país | `/pais bank deposit` e `/pais bank withdraw` |
| **Tesouro** | **Todas as outras moedas**: a sua e as de outros países | `/tesouro depositar` e `/tesouro sacar` |

Para ver tudo de uma vez:

```text
/tesouro
```

A primeira linha é o banco, em Geos. As seguintes são as outras moedas, com o valor aproximado de cada uma em Geos.

## O banco: os Geos do país

É o banco normal do país, o mesmo que paga a **manutenção** do território. Não existe um cofre separado para a economia.

```text
/pais bank deposit 50000
/pais bank withdraw 10000
```

É do banco que saem:

- a manutenção do território;
- os 70.000 Geos para [criar a moeda](/wiki/pais/economia/criar-moeda/);
- os 25.000 Geos para [criar o mercado](/wiki/pais/economia/criar-mercado/).

> **Atenção:** se você gastar todo o banco com a moeda e o mercado, pode faltar dinheiro para a manutenção. Deixe uma folga.

### Quanto custa ter um país

| | Valor |
|---|---|
| Fundar o país | 20.000 Geos, pagos por quem funda. Vem com 9 chunks |
| Cada chunk a mais | 1.000 Geos |
| Manutenção | 200 Geos por chunk, **todo dia às 15:00** |

> **Exemplo:** um país com 30 chunks paga 6.000 Geos por dia. Se o banco não tiver esse valor, o país perde os chunks mais novos até a conta fechar. O país em si não é apagado.

Um país novo não paga manutenção nos primeiros 2 dias. Os níveis 5, 7 e 9 do país deixam a manutenção 10% mais barata cada um.

## O tesouro: as outras moedas

Quando você cria a sua moeda, o tesouro recebe 100.000 unidades dela. Depois disso, entra dinheiro no tesouro quando:

- você [imprime](/wiki/pais/economia/imprimir-dinheiro/) mais moeda;
- alguém **compra** no mercado do país;
- alguém deposita.

E sai dinheiro quando:

- alguém **vende** no mercado do país (o tesouro é quem paga);
- alguém com permissão saca.

### Depositar e sacar

```text
/tesouro depositar 5000 PESO
/tesouro sacar 2000 PESO
```

O depósito tira da sua carteira e coloca no tesouro. O saque faz o contrário. Só quem pode mexer na economia do país usa esses comandos.

O tesouro guarda qualquer moeda, não só a sua. Se o seu país recebeu Francos, eles ficam lá também.

### Pagar pessoas

Para pagar um soldado, um construtor ou um aliado na moeda do país, saque do tesouro e pague pela sua carteira:

```text
/tesouro sacar 1000 PESO
/pagar Carlos 1000 PESO
```

## Fique de olho no saldo

O mercado do país paga os vendedores com o tesouro. **Se o tesouro zerar, o mercado para de comprar** e os jogadores veem a mensagem `ficou sem dinheiro`.

Um mercado que compra muito e vende pouco esvazia o tesouro. Veja como equilibrar em [Gerenciar o mercado](/wiki/pais/economia/gerenciar-mercado/).

## Ver o histórico

Cada entrada e saída do tesouro fica registrada: quem sacou, quem depositou, o que o mercado pagou. A administração do servidor consegue consultar esse histórico se houver uma disputa.
