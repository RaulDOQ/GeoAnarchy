---
title: Adotar uma moeda
summary: Usar a moeda de outro país sem criar a própria. E, se a moeda é sua, como decidir quem pode adotá-la.
order: 6
---

## O que é adotar

Um país que não quer gastar 70.000 Geos criando uma moeda pode **adotar a moeda de outro país**. Ela passa a ser a moeda oficial dele, mas quem controla continua sendo o país que a criou.

| | País que criou | País que adotou |
|---|---|---|
| Paga para criar a moeda | Sim | Não |
| A moeda é a oficial do país | Sim | Sim |
| O tesouro guarda a moeda | Sim | Sim |
| Pode imprimir | Sim | **Não** |
| Pode construir um mercado | Sim | **Não** |
| Sofre se o criador imprimir demais | Sim | Sim |

## Para quem quer adotar

```text
/currency adopt PESO
```

Dois resultados são possíveis:

- **A moeda é de adoção livre:** o seu país passa a usá-la na hora.
- **A moeda exige aprovação:** o pedido fica esperando. Os líderes do país dono da moeda recebem um aviso e decidem. Você é avisado da resposta se estiver online.

Para deixar de usar a moeda (ou cancelar um pedido que está esperando):

```text
/currency abandon
```

### Vantagens

- Não custa nada.
- O seu país passa a ter uma moeda que já tem valor e um mercado onde gastá-la.
- Os seus jogadores podem negociar no mercado do país dono com a moeda que já usam.

### Riscos

- **Você depende do outro país.** Se ele imprimir demais, a moeda perde valor e o seu tesouro perde junto.
- **Você não pode ter mercado.** Só o país que criou a moeda constrói mercados com ela.
- Se o país dono deixar de existir, a moeda continua valendo, mas ninguém mais imprime e os mercados dele somem.

Você pode mudar de ideia quando quiser: abandonar a moeda, adotar outra ou [criar a sua](/wiki/pais/economia/criar-moeda/). Criar a própria moeda encerra a adoção automaticamente.

## Para quem é dono da moeda

Ter outros países usando a sua moeda é bom para você: mais gente precisa dela, então ela vale mais, e o seu país ganha influência sobre os vizinhos.

### Escolher quem pode adotar

```text
/currency policy open
```

**Livre.** Qualquer país adota quando quiser. É como a moeda começa.

```text
/currency policy approval
```

**Com aprovação.** Cada país precisa pedir, e você aceita ou recusa.

### Responder aos pedidos

```text
/currency requests
```

Lista os países que estão esperando.

```text
/currency accept Argentina
/currency deny Argentina
```

Aceita ou recusa o pedido. Use o nome do país como aparece na lista.

Mudar a política não afeta os países que já adotaram a moeda.

### Você não pode expulsar um país

Depois que um país adotou a sua moeda, só ele pode deixar de usá-la. O que você controla é a impressão: é com ela que você ajuda ou prejudica quem depende da sua moeda.

## Se o comando for recusado

| Mensagem | Motivo |
|---|---|
| `tem moeda própria, então não pode adotar outra` | O seu país já criou uma moeda |
| `Todo país já usa a moeda mundial` | Você tentou adotar o Geo, que todos já usam |
| `não tem mais um país por trás` | O país dono da moeda deixou de existir |
| `Seu país já usa a moeda ...` | O seu país já adotou essa moeda |
| `Não há pedido de ...` | Não há pedido desse país. Confira em `/currency requests` |
