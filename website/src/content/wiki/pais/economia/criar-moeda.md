---
title: Criar uma moeda
summary: O comando, as regras para o nome, quanto custa e o que acontece no momento em que a moeda nasce.
order: 3
---

## O que você precisa

- Ser o dono do país, ou ter a permissão de economia.
- **70.000 Geos no banco do país.**
- O país ainda não pode ter uma moeda própria. Cada país cria **uma só**.

## O comando

```text
/moeda criar <CÓDIGO> <nome> <plural> [símbolo]
```

Exemplo:

```text
/moeda criar PESO Peso Pesos $
```

| Parte | Regra | Exemplo |
|---|---|---|
| **Código** | De 3 a 5 letras, sem números. É o que os jogadores digitam nos comandos | `PESO` |
| **Nome** | De 2 a 16 letras, uma palavra só | `Peso` |
| **Plural** | De 2 a 16 letras, uma palavra só | `Pesos` |
| **Símbolo** | Opcional, até 2 caracteres. Aparece antes dos valores | `$` |

O código e os nomes não podem ser iguais aos de uma moeda que já existe.

> **Atenção:** o nome, o código e o símbolo **não podem ser mudados depois**, e a moeda não pode ser apagada. Confira a escrita antes de enviar.

## Para onde vão os 70.000 Geos

| Valor | Destino |
|---|---|
| **50.000 Geos** | Taxa de criação. Esse dinheiro sai da economia |
| **20.000 Geos** | Ficam na **reserva de câmbio** da moeda, para sempre |

Os 20.000 Geos da reserva são o que dá valor à moeda no primeiro dia. Eles **não voltam** para o país: ficam travados na reserva, e é contra eles que os jogadores trocam a sua moeda.

## O que nasce junto com a moeda

No momento da criação são emitidas **300.000 unidades**:

| Quantidade | Onde fica |
|---|---|
| **200.000** | Na reserva de câmbio, junto com os 20.000 Geos |
| **100.000** | No tesouro do país, para você usar |

Com 20.000 Geos e 200.000 unidades na reserva, a taxa inicial é:

> **1 Geo = 10 unidades da sua moeda**

O servidor inteiro recebe um aviso de que o seu país criou uma moeda.

## Os primeiros passos depois de criar

1. Confira o tesouro com `/tesouro`: os 100.000 devem estar lá.
2. Veja a sua moeda em `/moeda info PESO`.
3. [Construa o mercado](/wiki/pais/economia/criar-mercado/). Sem ele, quase ninguém terá motivo para usar a moeda.
4. Decida se outros países podem [adotar a moeda](/wiki/pais/economia/adocao/) livremente ou só com a sua aprovação.

## Se o comando for recusado

| Mensagem | Motivo |
|---|---|
| `já tem a sua própria moeda` | O país já criou uma moeda |
| `O código precisa ter de 3 a 5 letras` | Código curto, longo ou com números |
| `O código ... já está em uso` | Já existe uma moeda com esse código |
| `Os nomes precisam ter de 2 a 16 letras` | Nome com números, símbolos ou tamanho errado |
| `Esse nome é parecido demais com uma moeda que já existe` | Já existe uma moeda com esse nome |
| `Criar uma moeda custa ...` | Faltam Geos no banco do país. Deposite com `/pais bank deposit` |
| `Você não comanda a economia de nenhum país` | Você não é o dono nem tem a permissão |

Quando o comando é recusado, nada é cobrado.

## Se o país tinha adotado outra moeda

Criar a moeda própria encerra a adoção automaticamente. A partir daí o país usa a moeda dele.
