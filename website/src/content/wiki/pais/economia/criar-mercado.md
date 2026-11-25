---
title: Criar um mercado
summary: O que é preciso para construir o mercado do país, como escolher o lugar e o que acontece depois.
order: 7
---

## O que você precisa

- **Uma moeda própria.** Só o país que criou uma moeda pode ter mercado. Um país que adotou a moeda de outro não pode.
- **25.000 Geos no banco do país.** É a taxa de construção, e esse dinheiro sai da economia.
- **Estar dentro do território do país**, no ponto onde o mercado vai ficar.
- O país ainda não ter um mercado. Cada país tem **um**.

## Escolher o lugar

O bloco onde você está pisando vira o **centro** do mercado. O ponto de venda alcança cerca de 10 blocos em todas as direções, inclusive para cima e para baixo.

Pense bem antes de construir:

- **Fácil de chegar.** Perto do litoral, de um rio ou de uma estrada. Ninguém atravessa uma cordilheira para vender trigo.
- **No nível do chão.** Como a área vale também na altura, um centro no alto de uma torre deixa o chão de fora.
- **Fácil de defender.** O ponto de venda não é protegido pelo servidor. Quem vem negociar precisa se sentir seguro, ou não volta.
- **Longe de outro mercado.** Precisa haver pelo menos 25 blocos entre dois mercados.

Dá para [mudar o lugar depois](/wiki/pais/economia/gerenciar-mercado/), sem pagar de novo.

## O comando

Vá até o ponto escolhido e digite:

```text
/mercado criar Madrid
```

O nome pode ter de 2 a 24 caracteres e pode ter espaços: `/mercado criar Porto de Lisboa`.

## O que acontece

- Os 25.000 Geos saem do banco do país.
- O mercado passa a existir, **aberto** e negociando na **moeda do país**.
- Aparece um anel de partículas na borda do ponto de venda, e quem entra recebe um aviso.
- O mercado aparece em `/mercado lista` para o servidor inteiro.
- O mercado passa a **seguir o mercado continental mais próximo**: os preços dele partem dos preços desse continente. Veja [Como o mercado define os preços](/wiki/pais/economia/precos-do-mercado/).

> **Importante:** um mercado novo **não negocia nenhum item** e tem o estoque vazio. O próximo passo é [escolher os itens](/wiki/pais/economia/itens-do-mercado/).

## Lista de conferência

Antes de anunciar o mercado para o servidor:

1. Adicione os itens que ele vai negociar.
2. Confira se o tesouro tem a moeda do país para pagar os vendedores (`/tesouro`).
3. Defina o imposto, se quiser cobrar um.
4. Abra o `/mercado` no ponto de venda e veja se os preços fazem sentido.

## Se o comando for recusado

| Mensagem | Motivo |
|---|---|
| `precisa de moeda própria para construir um mercado` | O país não criou uma moeda própria |
| `já tem o máximo de mercados permitido` | O país já tem um mercado |
| `O mercado de um país precisa ficar dentro do território dele` | Você está fora do território do país |
| `Isso é perto demais do mercado ...` | Há outro mercado a menos de 25 blocos |
| `Construir um mercado custa ...` | Faltam Geos no banco do país |
| `O nome precisa ter de 2 a 24 caracteres` | Nome curto ou longo demais |

Quando o comando é recusado, nada é cobrado.
